import scala.collection.parallel.CollectionConverters._
import data.Puzzle

import java.io.IOException
import java.net.URI
import java.nio.file.{FileSystems, Files, Path, Paths}

object PuzzleLoader {
  def load(): List[data.Puzzle] = {
    val folderURL = getClass.getResource("/puzzles.zip")
    extractAll(folderURL.toURI)
  }


  @throws[IOException]
  private def extractAll(fromZip: URI): List[data.Puzzle] = {
    import scala.jdk.CollectionConverters._
    import scala.jdk.StreamConverters._
    val fs = FileSystems.newFileSystem(Paths.get(fromZip.getPath), null)
    val files: List[Path] = fs.getRootDirectories.asScala.toList.flatMap(path =>
      Files.walk(path).toScala(List).filterNot(Files.isDirectory(_))
    )

    println("starting parse")
    val result = files.par.flatMap { path =>
      val contents = new String(Files.readAllBytes(path))
      val puzzle = Puzzle.parseXd(contents, path.toString)
      if (puzzle.isEmpty) {
        println(s"failed to parse file ${path.getFileName}")
      }
      puzzle
    }
    println("done parsing")
    result.toList
  }
}


