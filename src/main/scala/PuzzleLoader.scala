import scala.collection.parallel.CollectionConverters._
import data.Puzzle
import java.io.IOException
import java.nio.file.{FileSystems, Files, Path}
import java.nio.file.StandardCopyOption._
import scala.jdk.CollectionConverters._
import scala.jdk.StreamConverters._

object PuzzleLoader {
  def load(): List[data.Puzzle] = {
    val puzzles = getClass.getResourceAsStream("/puzzles.zip")
    val temp = os.temp().toNIO
    Files.copy(puzzles, temp, REPLACE_EXISTING)
    extractAll(temp)
  }

  @throws[IOException]
  private def extractAll(zipPath: Path): List[data.Puzzle] = {
    val fs = FileSystems.newFileSystem(zipPath, null)
    val files = fs.getRootDirectories.asScala.flatMap { path =>
      Files.walk(path).toScala(Seq).filterNot(Files.isDirectory(_))
    }

    val startTime = System.currentTimeMillis()
    println("starting parse")
    val result = files.par.flatMap { path =>
      val contents = new String(Files.readAllBytes(path))
      val puzzle = Puzzle.parseXd(contents, path.toString)
      if (puzzle.isEmpty) {
        println(s"failed to parse file ${path.getFileName}")
      }
      puzzle
    }.toList
    val elapsedTime = System.currentTimeMillis() - startTime
    println(s"parsed ${result.length} files in ${elapsedTime}ms")
    result
  }
}
