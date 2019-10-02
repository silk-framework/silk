package org.silkframework.plugins.dataset.csv

import java.io._

import com.univocity.parsers.csv.{CsvWriterSettings, CsvWriter => UniCsvWritter}
import org.silkframework.dataset.TypedProperty
import org.silkframework.runtime.resource.{FileResource, WritableResource}

/**
  * Encapsulates the CSV writting.
  *
  * @param resource The resource to be written to
  * @param properties The properties that build the columns
  * @param settings The csv settings
  */
class CsvWriter(resource: WritableResource, properties: Seq[TypedProperty], settings: CsvSettings) {

  // The underlying Java IO Writer
  private val writer: Writer = resource match {
    case f: FileResource =>
      // Use a buffered writer that directly writes to the file
      f.file.getParentFile.mkdirs()
      new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.file), "UTF-8"))
    case _ =>
      new StringWriter()
  }

  // The Univocity CSV Writer
  private val csvWriter = createCsvWriter()
  csvWriter.writeHeaders()

  /**
    * Writes a row.
    */
  def writeLine(values: Seq[String]): Unit = {
    csvWriter.writeRow(values: _*)
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
    // If we are using a string writer, we still need to write the data to the resource
    writer match {
      case stringWriter: StringWriter =>
        resource.writeString(stringWriter.toString)
      case _ =>
    }
  }

  /**
    * Creates a Univocity CSV writer with the given settings.
    */
  private def createCsvWriter() = {
    val writerSettings = new CsvWriterSettings
    writerSettings.getFormat.setLineSeparator("\n")
    if(properties.nonEmpty) {
      writerSettings.setHeaders(properties.map(_.propertyUri): _*)
    }
    writerSettings.getFormat.setDelimiter(settings.separator)

    for(quoteChar <- settings.quote) {
      writerSettings.getFormat.setQuote(quoteChar)
    }

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
