package http

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.ActorSink
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import data.Puzzle
import realtime.{Game, GameSupervisor}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Success}

class WebServer(val puzzles: List[Puzzle]) extends HttpApp with DefaultJsonProtocol with SprayJsonSupport {

  val system: ActorSystem[SpawnProtocol.Command] =
    ActorSystem(GameSupervisor.main, "game-supervisor")
  private val log = system.log
  implicit val ec: ExecutionContext = system.executionContext

  def getQuerySet(maybeQuery: Option[String]): List[Puzzle] = maybeQuery match {
    case Some(query) =>
      puzzles.filter { puzzle =>
        puzzle.title match {
          case Some(title) => title.toLowerCase().contains(query.toLowerCase())
          case None => false
        }
      }
    case None => puzzles
  }

  private def makeWebsocketHandler(): Future[Flow[Message, Message, NotUsed]] = {
    // heavily inspired by https://github.com/calvinlfer/websockets-pubsub-akka

    import scala.concurrent.duration._
    implicit val timeout: Timeout = Timeout(3.seconds)
    implicit val scheduler: Scheduler = system.scheduler

    log.debug("trying to spawn a game actor")

    // the main docs were out of date, but these tests contained usable examples of SpawnProtocol
    // https://github.com/akka/akka/commit/d03294d359497c7c7f0e7615277a2f1b733e3c0c
    val gameFuture: Future[ActorRef[Game.Command]] = system.ask(
      SpawnProtocol.Spawn(Game.main, "game", Props.empty, _)
    )

    gameFuture.map { gameRef =>
      log.debug(s"game future completed with $gameRef")

      val sink: Sink[Message, NotUsed] =
        Flow[Message].collect {
          case TextMessage.Strict(txt) => Game.WSMessage(txt)
        }.to(
          ActorSink.actorRef[Game.Command](
            gameRef,
            Game.WSHandleDropped,
            Game.HandleError
          )
        )

      val source: Source[Message, NotUsed] =
        Source.actorRef(
          bufferSize = 10,
          overflowStrategy = OverflowStrategy.dropBuffer
        ).mapMaterializedValue { wsHandle =>
          gameRef ! Game.WSConnected(wsHandle)
          NotUsed
        }.keepAlive(
          maxIdle = 10.seconds,
          () => TextMessage.Strict("ping!\n")
        )

      Flow.fromSinkAndSource(sink, source)
    }
  }

  override def routes: Route = cors() {
    concat(
      (pathEndOrSingleSlash & get) {
        complete("hello squirrel")
      },
      (pathPrefix("puzzles") & pathEndOrSingleSlash & parameters('n.?, 'q.?) & get) {
        (maybeN, maybeQ) =>
          val querySet = getQuerySet(maybeQ)
          val howMany = maybeN.flatMap(_.toIntOption).getOrElse(1) min 32 min querySet.length

          // shuffling the entire list is probably not very efficient, but we need to avoid picking duplicates
          // maybe revisit this later
          val picks = Random.shuffle(querySet).take(howMany).map(puzzle =>
            Map("id" -> puzzle.id, "puzzle" -> puzzle.rawText, "title" -> puzzle.title.getOrElse(""))
          )
          complete(picks.toJson)
      },
      (pathPrefix("puzzles") & pathPrefix(Segment) & get) { puzzleId =>
        encodeResponse {
          puzzles.find(_.id == puzzleId) match {
            case Some(puzzle) => complete(puzzle.rawText)
            case None => complete(HttpResponse(status = StatusCodes.NotFound))
          }
        }
      },
      path("ws") {
        log.debug("incoming websocket connection")
        onComplete(makeWebsocketHandler()) {
          case Success(value) =>
            handleWebSocketMessages(value)
          case Failure(err) =>
            complete(HttpResponse(StatusCodes.InternalServerError, entity = HttpEntity(err.getMessage)))
        }
      }
    )
  }
}
