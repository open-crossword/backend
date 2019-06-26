import akka.http.scaladsl.server.{HttpApp, Route}
import spray.json._

import scala.util.Random

object Server extends HttpApp with DefaultJsonProtocol {
  private val loader = new PuzzleLoader()
  private val puzzles: Seq[Puzzle] = loader.load()

  override protected def routes: Route =
    pathPrefix("puzzles") {
      concat(
        pathEndOrSingleSlash {
          get {
            val picks = for (_ <- 0 to 10) yield {
              puzzles(Random.nextInt(puzzles.length)).fullText
            }
            complete(picks.toJson.prettyPrint)
          }
        },
        encodeResponse {
          getFromDirectory(loader.xdPath.toString())
        }
      )
    }

  def main(args: Array[String]): Unit = {
    Server.startServer("localhost", 8080)
  }
}

