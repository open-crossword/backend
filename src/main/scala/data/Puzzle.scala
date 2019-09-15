package data

import cats.implicits._
import utils._

case class Date(day: Int, month: Int, year: Int)

case class Puzzle(title: Option[String],
                  copyright: Option[String],
                  date: Option[Date],
                  author: Option[String],
                  editor: Option[String],
                  publication: Option[String],
                  rawText: String,
                  id: String
                 )

object Puzzle {
  def parseXd(xdString: String, path: String): Option[Puzzle] = {
    // really dislike needing to pass the path
    val sections = xdString.split("\n\n")
    val metadataSection = sections.headOption
    metadataSection.flatMap { metadataChunk =>
      val lineRegex = "(.*): (.*)".r
      metadataChunk.split('\n').toList.traverse {
        case lineRegex(a, b) => Some(a, b)
        case _ => None
      }.map(_.toMap).map { map =>
        Puzzle(
          map.get("Title"),
          map.get("Copyright"),
          map.get("Date").flatMap(parseDate),
          map.get("Author"),
          map.get("Editor"),
          publicationForPath(path),
          xdString,
          md5(path) // might be better to try to generate a human-readable ID
        )
      }
    }
  }

  // credit:
  // https://alvinalexander.com/source-code/scala-method-create-md5-hash-of-string
  def md5(s: String): String = {
    import java.security.MessageDigest
    import java.math.BigInteger
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1, digest)
    val hashedString = bigInt.toString(16)
    hashedString
  }


  private def parseDate(value: String): Option[Date] =
    value.split('-') match {
      case Array(Int(year), Int(month), Int(day)) => Some(Date(day, month, year))
      case _ => None
    }

  private def publicationForPath(path: String): Option[String] = {
    val pathRegex = ".*/xd/(.+?)/.*".r
    path match {
      case pathRegex(a) => Some(a)
      case _ => None
    }
  }
}
