import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{HttpApp, Route}
import spray.json._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

object WebServer extends HttpApp with DefaultJsonProtocol with SprayJsonSupport {

  override def routes: Route = cors() {
    concat(
      (pathEndOrSingleSlash & get) {
        complete("hello squirrel")
      },
      (pathPrefix("puzzles") & pathEndOrSingleSlash & get) {
        complete("hello puzzles")
      },
      (pathPrefix("puzzles") & get) {
        complete("unknown1")
      })
  }

  def main(args: Array[String]): Unit = {
    WebServer.startServer("0.0.0.0", 8080)
  }
}
