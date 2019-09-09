import data.{Puzzle, Date}
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class PuzzleTest extends FlatSpec with Matchers {

  behavior of "Puzzle.parseXd"

  it should "parse an XD formatted file" in {
    val xdString = Source.fromResource("sample-xd-file.xd").mkString
    val path = "/foo/bar/xd/bostonglobe/1917/bg1917-02-25.xd"
    val puzzle = Puzzle.parseXd(xdString, path)
    puzzle match {
      case Some(p) =>
        assert(p.author.isEmpty)
        assert(p.date.contains(Date(25, 2, 1917)))
        assert(p.editor.isEmpty)
        assert(p.title.contains("CROSS-WORD PUZZLE of February 25, 1917"))
        assert(p.copyright.contains("© 1917 Boston Globe"))
        assert(p.publication.contains("bostonglobe"))
        assert(p.rawText == xdString)
        assert(p.id == path)
      case None => fail()
    }
  }

  it should "return None if passed something bogus" in {
    assert(Puzzle.parseXd("WOW I AM NOT LEGIT", "").isEmpty)
  }
}
