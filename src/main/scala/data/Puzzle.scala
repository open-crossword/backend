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
          // Using path as the id seems wrong.
          // Maybe we should hash xdString, or use an incrementing number.
          path
        )
      }
    }
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
