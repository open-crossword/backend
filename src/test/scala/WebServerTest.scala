import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

class WebServerTest extends FlatSpec with Matchers with ScalatestRouteTest with DefaultJsonProtocol with SprayJsonSupport {

  behavior of "The /puzzles endpoint"

  it should "say hello" in {
    Get("/puzzles") ~> WebServer.routes ~> check {
      responseAs[String] shouldEqual "hello puzzles"
    }
  }
}
