package org.silkframework.plugins.dataset.csv

import com.univocity.parsers.csv.{CsvWriterSettings, CsvWriter => UniCsvWritter}
import org.silkframework.dataset.TypedProperty
import org.silkframework.runtime.resource.WritableResource

import java.io._

/**
  * Encapsulates the CSV writing.
  *
  * @param resource The resource to be written to
  * @param properties The properties that build the columns
  * @param settings The csv settings
  */
class CsvWriter(resource: WritableResource, properties: Seq[TypedProperty], settings: CsvSettings) {

  // The underlying Java IO Writer
  private val writer: Writer = new OutputStreamWriter(resource.createOutputStream(), settings.codec.charSet)

  // The Univocity CSV Writer
  private val csvWriter = createCsvWriter()
  csvWriter.writeHeaders()

  /**
    * Writes a row.
    */
  def writeLine(values: Seq[String]): Unit = {
    val sanitizedRow = values.map(CSVSanitizer.sanitize)
    csvWriter.writeRow(sanitizedRow: _*)
  }

  /**
    * Closes this writer.
    */
  def close(): Unit = {
    try {
      csvWriter.close()
    } finally {
      writer.close()
    }
  }

  /**
    * Creates a Univocity CSV writer with the given settings.
    */
  private def createCsvWriter() = {
    val writerSettings = new CsvWriterSettings

    // Allow the processing of non-printable characters.
    writerSettings.trimValues(false)

    writerSettings.getFormat.setLineSeparator("\n")
    if(properties.nonEmpty) {
      writerSettings.setHeaders(properties.map(_.propertyUri.uri): _*)
    }
    writerSettings.getFormat.setDelimiter(settings.separator)

    for(quoteChar <- settings.quote) {
      writerSettings.getFormat.setQuote(quoteChar)
    }
    writerSettings.getFormat.setQuoteEscape(settings.quoteEscapeChar)

    settings.maxCharsPerColumn foreach {
      writerSettings.setMaxCharsPerColumn
    }
    settings.maxColumns foreach {
      writerSettings.setMaxColumns
    }
    settings.commentChar foreach {
      writerSettings.getFormat.setComment(_)
    }
    settings.nullValue foreach writerSettings.setNullValue

    new UniCsvWritter(writer, writerSettings)
  }

}
