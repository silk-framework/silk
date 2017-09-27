package org.silkframework.plugins.dataset.csv

import java.net.URLEncoder

import org.silkframework.util.Uri

import scala.collection.immutable
import scala.util.matching.Regex

object CsvSourceHelper {
  final val UNNAMED_COLUMN_PREFIX = "unnamed_col"
  val unnamedRegex: Regex = s"""$UNNAMED_COLUMN_PREFIX([1-9]\\d*)(_[1-9]\\d*)?""".r

  lazy val standardCsvParser = new CsvParser(
    Seq.empty,
    CsvSettings(quote = Some('"'))
  )

  def serialize(fields: Traversable[String]): String = {
    fields.map { field =>
      if (field.contains("\"") || field.contains(",")) {
        escapeString(field)
      } else {
        field
      }
    }.mkString(",")
  }

  def parse(str: String): Seq[String] = {
    standardCsvParser.synchronized {
      standardCsvParser.parseLine(str)
    }
  }

  def escapeString(str: String): String = {
    val quoteReplaced = str.replaceAll("\"", "\"\"")
    s"""\"$quoteReplaced\""""
  }

  /** Finds existing columns in the CSV header fields that follow the unnamed column name schema. Used later to prevent name clashes. */
  private def unnamedColumnClashes(headerFields: Array[String]): Map[Int, Int] = {
    val entries: Seq[Option[(Int, Int)]] = headerFields.toSeq.filter(f => Option(f).isDefined).map {
      case unnamedRegex(colNr, offset) =>
        Some((colNr.toInt, Option(offset).map(_.drop(1).toInt).getOrElse(1)))
      case _ =>
        None
    }
    entries.flatten.
        groupBy(_._1).
        mapValues(_.map(_._2).max)
  }

  /** Converts the field names to a representation that can be used in URIs */
  def convertHeaderFields(headerFields: Array[String], prefix: String): immutable.IndexedSeq[String] = {
    val existingUnnamedMap: Map[Int, Int] = unnamedColumnClashes(headerFields)
    headerFields.zipWithIndex
        .map {
          case (null, idx) =>
            val colIdx = idx + 1
            val columnName = UNNAMED_COLUMN_PREFIX + colIdx
            existingUnnamedMap.get(colIdx) match {
              case Some(max) => columnName + "_" + (max + 1)
              case None => columnName
            }
          case (s, _) =>
            if (Uri(s).isValidUri) {
              s
            } else {
              URLEncoder.encode(s, "UTF-8")
            }
        }.toIndexedSeq
  }
}