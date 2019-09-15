import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{HttpApp, Route}
import spray.json._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import data.Puzzle

import scala.util.Random

class WebServer(val puzzles: List[Puzzle]) extends HttpApp with DefaultJsonProtocol with SprayJsonSupport {

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
      })
  }

}
object WebServer {
  def main(args: Array[String]): Unit = {
    val puzzles: List[Puzzle] = PuzzleLoader.load()
    new WebServer(puzzles).startServer("0.0.0.0", 8080)
  }
}
