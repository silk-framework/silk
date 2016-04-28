package org.silkframework.plugins.dataset.csv

import org.silkframework.dataset._
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.resource.{WritableResource, Resource}

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
    file: WritableResource,
  @Param("Comma-separated list of URL-encoded properties. If not provided, the list of properties is read from the first line.")
    properties: String = "",
  @Param("The character that is used to separate values. If not provided, defaults to ',', i.e., comma-separated values. '\\t' for specifying tab-separated values, is also supported.")
    separator: String = ",",
  @Param("The character that is used to separate the parts of array values.")
    arraySeparator: String = "",
  @Param("Character used to quote values.")
    quote: String = "\"",
  @Param(" A URI prefix that should be used for generating schema entities like classes or properties, e.g. http://www4.wiwiss.fu-berlin.de/ontology/")
    prefix: String = "",
  @Param("A pattern used to construct the entity URI. If not provided the prefix + the line number is used. An example of such a pattern is 'urn:zyx:{id}' where *id* is a name of a property.")
    uri: String = "",
  @Param("A regex filter used to match rows from the CSV file. If not set all the rows are used.")
    regexFilter: String = "",
  @Param("The file encoding, e.g., UTF8, ISO-8859-1")
    charset: String = "UTF-8",
  @Param("The number of lines to skip in the beginning, e.g. copyright, meta information etc.")
    linesToSkip: Int = 0,
  @Param("The maximum characters per column. If there are more characters found, the parser will fail.")
    maxCharsPerColumn: Int = 4096,
  @Param("If set to true then the parser will ignore lines that have syntax errors or do not have to correct number of fields according to the current config.")
    ignoreBadLines: Boolean = false) extends DatasetPlugin with DatasetPluginAutoConfigurable[CsvDataset] {

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

  private val settings = CsvSettings(sepChar, arraySeparatorChar, quoteChar, maxCharsPerColumn = Some(maxCharsPerColumn))

  override def source: DataSource = new CsvSource(file, settings, properties, prefix, uri, regexFilter, codec, skipLinesBeginning = linesToSkip, ignoreBadLines = ignoreBadLines)

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
    // Skip one more line if header was detected and property list set
    val skipHeader = if (csvSource.propertyList.size > 0) 1 else 0
    CsvDataset(
      file = file,
      properties = CsvSourceHelper.serialize(csvSource.propertyList),
      separator = if (detectedSeparator == "\t") "\\t" else detectedSeparator,
      arraySeparator = arraySeparator,
      quote = quote,
      prefix = prefix,
      uri = uri,
      regexFilter = regexFilter,
      charset = csvSource.codecToUse.name,
      linesToSkip = csvSource.skipLinesAutomatic.map(_ + skipHeader).getOrElse(linesToSkip)
    )
  }
}