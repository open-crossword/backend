import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{HttpApp, Route}
import spray.json._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import data.Puzzle

import scala.util.Random

object WebServer extends HttpApp with DefaultJsonProtocol with SprayJsonSupport {
  val puzzles: List[Puzzle] = PuzzleLoader.load()

  override def routes: Route = cors() {
    concat(
      (pathEndOrSingleSlash & get) {
        complete("hello squirrel")
      },
      (pathPrefix("puzzles") & pathEndOrSingleSlash & parameter('n.?) & get) { maybeN =>
        val howMany = maybeN.flatMap(_.toIntOption).getOrElse(1) min 32
        val picks = for (_ <- 1 to howMany) yield {
          val puzzle = puzzles(Random.nextInt(puzzles.length))
          Map("id" -> puzzle.id, "puzzle" -> puzzle.rawText)
        }
        complete(picks.toJson)
      },
      (pathPrefix("puzzles") & pathPrefix(Segment) & get) { puzzleId =>
        encodeResponse {
          puzzles.find(_.id == puzzleId) match {
            case Some(puzzle) => complete(puzzle.rawText)
            case None => complete(HttpResponse(status = StatusCodes.NotFound))
          }
        }
      })
  }

  def main(args: Array[String]): Unit = {
    WebServer.startServer("0.0.0.0", 8080)
  }
}
