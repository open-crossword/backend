import data.Puzzle
import http.WebServer

object Main extends App {
  val puzzles: List[Puzzle] = PuzzleLoader.load()
  new WebServer(puzzles).startServer("", 8080)
}
