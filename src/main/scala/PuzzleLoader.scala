import cats.implicits._
import os.Path

case class Date(day: Int, month: Int, year: Int)

case class Puzzle(date: Option[Date],
                  author: Option[String],
                  editor: Option[String],
                  publication : Option[String],
                  fullText: String,
                  path: String
                 )

class PuzzleLoader {
  val puzzlesHome: Path = os.home / ".config" / "crossword-server" / "puzzles"
  val xdPath: Path = puzzlesHome / "xd"

  private def setupPuzzlesDir(puzzlesHome: os.Path): Unit = {
    println(s"creating $puzzlesHome...")
    os.makeDir.all(puzzlesHome)
    println("downloading puzzles...")
    val r = requests.get("http://xd.saul.pw/xd-public.zip")
    val xdZipPath = puzzlesHome / "xd-public.zip"
    os.write(xdZipPath, r.data.bytes)
    try {
      println("unzipping...")
      os.proc("unzip", "xd-public.zip").spawn(cwd = puzzlesHome).wait()
      println("done")
    } finally {
      os.remove(xdZipPath)
    }
  }

  private def parseXdMetadata(filePath: os.Path): Either[os.Path, Puzzle] = {
    val rawPuzzle = os.read(filePath)
    val head = rawPuzzle.split("\n\n").headOption
    head.flatMap { chunk =>
      val entryRegex = "(.*): (.*)".r
      // (a -> Maybe b) -> List a -> Maybe (List b)
      chunk.split('\n').toList.traverse {
        case entryRegex(a, b) => Some (a, b)
        case _ => None
      }.map(_.toMap).map { map =>
        Puzzle(
          map.get("Date").flatMap(parseDate),
          map.get("Author"),
          map.get("Editor"),
          publicationForPath(filePath.toString()),
          rawPuzzle,
          filePath.relativeTo(xdPath).toString()
        )
      }
    }.toRight(filePath)
  }

  private def parseDate(value: String): Option[Date] =
    value.split('-') match {
      case Array(Int(year), Int(month), Int(day)) => Some(Date(year, month, day))
      case _ => None
    }

  private def publicationForPath(path: String): Option[String] = {
    val pathRegex = ".*/xd/(.+?)/.*".r
    path match {
      case pathRegex(a) => Some(a)
      case _ => None
    }
  }

  def load(): List[Puzzle] = {
    if (!os.exists(xdPath)) {
      setupPuzzlesDir(puzzlesHome)
    }

    os.walk.stream(xdPath)
      .filter(_.ext == "xd")
      .toList
      .map(parseXdMetadata)
      .mapFilter {
        case Left(err) =>
          println("failed to parse puzzle: " + err)
          None
        case Right(ok) => Some(ok)
      }
  }
}

object Int {
  def unapply(s: String): Option[Int] = util.Try(s.toInt).toOption
}

