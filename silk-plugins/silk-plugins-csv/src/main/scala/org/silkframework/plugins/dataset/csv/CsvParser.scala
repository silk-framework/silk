package org.silkframework.plugins.dataset.csv

import java.io.Reader
import java.util.logging.Logger

import com.univocity.parsers.csv.{CsvParserSettings, CsvParser => UniCsvParser}

import scala.util.Try

class CsvParser(selectedIndices: Seq[Int], settings: CsvSettings) {
  private final val MAX_CHARS_PER_COLUMNS_DEFAULT = 100000
  private final val MAX_COLUMNS_DEFAULT = 100000
  private val parserSettings = new CsvParserSettings()
  private val log: Logger = Logger.getLogger(classOf[CsvParser].getName)
  import settings._
  parserSettings.getFormat.setDelimiter(separator)
  parserSettings.setLineSeparatorDetectionEnabled(true)

  for(quoteChar <- quote) {
    parserSettings.getFormat.setQuote(quoteChar)
  }
  if(selectedIndices.nonEmpty) {
    parserSettings.selectIndexes(selectedIndices.map(new Integer(_)): _*)
  }
  parserSettings.setMaxCharsPerColumn(maxCharsPerColumn.getOrElse(MAX_CHARS_PER_COLUMNS_DEFAULT))
  parserSettings.setMaxColumns(maxColumns.getOrElse(MAX_COLUMNS_DEFAULT))
  parserSettings.getFormat.setQuoteEscape(settings.quoteEscapeChar)

  commentChar match {
    case Some(c) =>
      parserSettings.getFormat.setComment(c)
    case None =>
      // We need to explicitly disable the comment char, as it defaults to '#'
      parserSettings.getFormat.setComment('\u0000')
  }

  private val parser = new UniCsvParser(parserSettings)

  /** Initializes this parser with a reader. This must be done before calling readNext */
  def beginParsing(reader: Reader): Unit = {
    parser.beginParsing(reader)
  }

  /**
    * Returns the next entry from the CSV file. beginParsing must be called before calling this method.
    * If it reached the end of the [[java.io.Reader]] it will return None.
    */
  def parseNext(): Option[Array[String]] = {
    Option(parser.parseNext())
  }

  /** Stops parsing and closes all open resources. */
  def stopParsing(): Unit = {
    val result = Try(parser.stopParsing())
    if(result.isFailure) {
      log.warning("Some error occurred during stopping the CSV Parser. Error message: " + result.failed.get.getMessage)
    }
  }

  def parseLine(line: String): Seq[String] = {
    parser.parseLine(line)
  }

}
