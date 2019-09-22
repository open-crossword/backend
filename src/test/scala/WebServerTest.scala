import PuzzleLoader.getClass
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import http.WebServer

class WebServerTest extends FlatSpec with Matchers with ScalatestRouteTest with DefaultJsonProtocol with SprayJsonSupport {
  val WebServer = new WebServer(PuzzleLoader.load(getClass.getResourceAsStream("/sample-puzzles-dir.zip")))

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

  it should "allow us to search by title using a query param" in {
    val query = "HEADLINE"
    Get(s"/puzzles?q=$query") ~> WebServer.routes ~> check {
      val response = responseAs[List[Map[String, String]]]
      assert(response.nonEmpty)
      response.foreach(it =>
        assert(it("title").contains(query)))
    }
  }

  it should "not return more search results than actually exist" in {
    val query = "HEADLINE"
    Get(s"/puzzles?q=$query&n=32") ~> WebServer.routes ~> check {
      val response = responseAs[List[Map[String, String]]]
      assert(response.length == 2)
    }
  }

  it should "not return the same result twice when searching" in {
    val query = "HEADLINE"
    // not an especially rigorous test given that the results are randomized,
    // maybe we should set a seed for the RNG, or mock it, or something else.
    for(_ <- 0 to 20) {
      Get(s"/puzzles?q=$query&n=2") ~> WebServer.routes ~> check {
        val response = responseAs[List[Map[String, String]]]
        val d = response.distinctBy(_("id"))
        assert(d.length == response.length)
      }
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
