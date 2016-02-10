package org.silkframework.plugins.dataset.csv

import java.io.File

import org.silkframework.dataset._
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.resource.{FileResource, Resource}

import scala.io.Codec

@Plugin(
  id = "csv",
  label = "CSV",
  description =
      """Retrieves all entities from a csv file."""
)
case class CsvDataset
(
    @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
    file: Resource,
    @Param("Separated list of properties by the separator parameter. If not provided, the list of properties is read from the first line.")
    properties: String = "",
    @Param("The character that is used to separate values. If not provided, defaults to ',', i.e., comma-separated values. '\t' for specifying tab-separated values, is also supported.")
    separator: String = ",",
    @Param("The character that is used to separate the parts of array values.")
    arraySeparator: String = "",
    @Param("Character used to quote values.")
    quote: String = "",
    @Param(" A URI prefix that should be used for generating schema entities like classes or properties, e.g. http://www4.wiwiss.fu-berlin.de/ontology/")
    prefix: String = "",
    @Param("A pattern used to construct the entity URI. If not provided the prefix + the line number is used. An example of such a pattern is 'urn:zyx:{id}' where *id* is a name of a property.")
    uri: String = "",
    @Param("A regex filter used to match rows from the CSV file. If not set all the rows are used.")
    regexFilter: String = "",
    @Param("The file encoding, e.g., UTF8, ISO-8859-1")
    charset: String = "UTF-8",
    @Param("The number of lines to skip in the beginning, e.g. copyright, meta information etc.")
    linesToSkip: Int = 0) extends DatasetPlugin with PluginAutoConfigurable[CsvDataset] {

  private val sepChar =
    if (separator == "\\t") '\t'
    else if (separator.length == 1) separator.head
    else throw new IllegalArgumentException(s"Invalid separator: '$separator'. Must be a single character.")

  private val arraySeparatorChar =
    if (arraySeparator.isEmpty) None
    else if (arraySeparator.length == 1) Some(arraySeparator.head)
    else throw new IllegalArgumentException(s"Invalid array separator character: '$arraySeparator'. Must be a single character.")

  private val quoteChar =
    if (quote.isEmpty) None
    else if (quote.length == 1) Some(quote.head)
    else throw new IllegalArgumentException(s"Invalid quote character: '$quote'. Must be a single character.")

  private val codec = Codec(charset)

  private val settings = CsvSettings(sepChar, arraySeparatorChar, quoteChar)

  override def source: DataSource = new CsvSource(file, settings, properties, prefix, uri, regexFilter, codec, skipLinesBeginning = linesToSkip)

  override def linkSink: LinkSink = new CsvLinkSink(file, settings)

  override def entitySink: EntitySink = new CsvEntitySink(file, settings)

  override def clear(): Unit = {}

  /**
   * returns an auto-configured version of this plugin
   */
  override def autoConfigured: CsvDataset = {
    val csvSource = new CsvSource(file, settings, properties, prefix, uri, regexFilter, codec,
      detectSeparator = true, detectSkipLinesBeginning = true, fallbackCodecs = List(Codec.ISO8859), maxLinesToDetectCodec = Some(1000))
    val detectedSettings = csvSource.csvSettings
    val detectedSeparator = detectedSettings.separator.toString
    CsvDataset(
      file = file,
      properties = csvSource.propertyList.mkString(detectedSeparator),
      separator = detectedSeparator,
      arraySeparator = arraySeparator,
      quote = quote,
      prefix = prefix,
      uri = uri,
      regexFilter = regexFilter,
      charset = csvSource.codecToUse.name,
      linesToSkip = csvSource.skipLinesAutomatic.getOrElse(linesToSkip)
    )
  }
}