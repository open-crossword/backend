import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.server.{HttpApp, Route}
import spray.json._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

import scala.util.Random

object Server extends HttpApp with DefaultJsonProtocol with SprayJsonSupport {
  private val loader = new PuzzleLoader()
  private val puzzles: Seq[Puzzle] = loader.load()

  override protected def routes: Route = cors() {
    pathPrefix("puzzles") {
      concat(
        pathEndOrSingleSlash {
          get {
            val picks = for (_ <- 1 to 8) yield {
              val puzzle = puzzles(Random.nextInt(puzzles.length))
              Map(
                "id" -> puzzle.id,
                "puzzle" -> puzzle.fullText,
              )
            }
            complete(picks.toJson)
          }
        },
        pathPrefix(Segment) { puzzleId =>
          encodeResponse {
            puzzles.find(_.id == puzzleId) match {
              case Some(puzzle) => complete(puzzle.fullText)
              case None => complete(HttpResponse(status = StatusCodes.NotFound))
            }
          }
        }
      )
    }
  }

  def main(args: Array[String]): Unit = {
    Server.startServer("localhost", 8080)
  }
}
