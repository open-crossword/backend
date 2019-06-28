import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
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
                "name" -> os.RelPath(puzzle.path).baseName,
                "puzzle" -> puzzle.fullText,
              )
            }
            complete(picks.toJson)
          }
        },
        encodeResponse {
          getFromDirectory(loader.xdPath.toString())
        }
      )
    }
  }

  def main(args: Array[String]): Unit = {
    Server.startServer("localhost", 8080)
  }
}
