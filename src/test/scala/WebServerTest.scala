import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

class WebServerTest extends FlatSpec with Matchers with ScalatestRouteTest with DefaultJsonProtocol with SprayJsonSupport {

  behavior of "The /puzzles endpoint"

  it should "give us a random puzzle when accessed without a query param" in {
    Get("/puzzles") ~> WebServer.routes ~> check {
      val response = responseAs[List[Map[String, String]]]
      assert(response.size == 1)
      val entry = response(0)
      assert(entry("id").length > 0)
      assert(!entry("id").contains("/"))

      val source = WebServer.puzzles.find(_.id == entry("id"))
      assert(source.isDefined)
      assert(source.get.rawText == entry("puzzle"))
    }
  }

  it should "accept a query parameter to control the number of puzzles returned" in {
    Get("/puzzles?n=5") ~> WebServer.routes ~> check {
      val response = responseAs[List[Map[String, String]]]
      assert(response.size == 5)
    }
  }

  it should "not give us more than 32 puzzles" in {
    Get("/puzzles?n=33") ~> WebServer.routes ~> check {
      val response = responseAs[List[Map[String, String]]]
      assert(response.size == 32)
    }
  }

  it should "allow us to fetch a puzzle by ID" in {
    val puzzle = WebServer.puzzles(0)
    Get(s"/puzzles/${puzzle.id}") ~> WebServer.routes ~> check {
      val response = responseAs[String]
      assert(response == puzzle.rawText)
    }
  }
}
