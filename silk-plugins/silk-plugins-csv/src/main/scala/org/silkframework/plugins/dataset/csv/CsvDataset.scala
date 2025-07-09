package org.silkframework.plugins.dataset.csv

import org.silkframework.dataset.DatasetCharacteristics.{SpecialPaths, SupportedPathExpressions}
import org.silkframework.dataset._
import org.silkframework.dataset.bulk.TextBulkResourceBasedDataset
import org.silkframework.plugins.dataset.charset.CharsetAutocompletionProvider
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.{ParameterObjectValue, ParameterStringValue, ParameterValues, PluginContext}
import org.silkframework.runtime.resource._

@Plugin(
  id = "csv",
  label = "CSV",
  categories = Array(DatasetCategories.file),
  description =
      """Read from or write to an CSV file."""
)
case class CsvDataset (
  @Param("The CSV file. This may also be a zip archive of multiple CSV files that share the same schema.")
    file: WritableResource,
  @Param("Comma-separated list of properties. If not provided, the list of properties is read from the first line. Properties that are no valid (relative or absolute) URIs will be encoded.")
    properties: String = "",
  @Param("The character that is used to separate values. If not provided, defaults to ',', i.e., comma-separated values. \"\\t\" for specifying tab-separated values, is also supported.")
    separator: String = ",",
  @Param(label = "Array separator", value = "The character that is used to separate the parts of array values. Write \"\\t\" to specify the tab character.")
    arraySeparator: String = "",
  @Param("Character used to quote values.")
    quote: String = "\"",
  @deprecated("This will be removed in the next release.", "")
  @Param(label = "URI pattern", value = "*Deprecated* A pattern used to construct the entity URI. If not provided the prefix + the line number is used. An example of such a pattern is 'urn:zyx:{id}' where *id* is a name of a property.", advanced = true)
    uri: String = "",
  @Param(value = "The file encoding, e.g., UTF-8, UTF-8-BOM, ISO-8859-1", autoCompletionProvider = classOf[CharsetAutocompletionProvider])
    charset: String = "UTF-8",
  @Param("A regex filter used to match rows from the CSV file. If not set all the rows are used.")
    regexFilter: String = "",
  @Param("The number of lines to skip in the beginning, e.g. copyright, meta information etc.")
    linesToSkip: Int = 0,
  @Param(value = "The maximum characters per column. *Warning*: System will request heap memory of that size (2 bytes per character) when reading the CSV. If there are more characters found, the parser will fail.", advanced = true)
    maxCharsPerColumn: Int = CsvDataset.DEFAULT_MAX_CHARS_PER_COLUMN,
  @Param("If set to true then the parser will ignore lines that have syntax errors or do not have to correct number of fields according to the current config.")
    ignoreBadLines: Boolean = false,
  @Param(label = "Quote escape character",
    value = "Escape character to be used inside quotes, used to escape the quote character. It must also be used to escape itself, e.g. by doubling it, e.g. \"\". If left empty, it defaults to quote.")
  quoteEscapeCharacter: String = "\"",
  @Param(label = "ZIP file regex", value = "If the input resource is a ZIP file, files inside the file are filtered via this regex.", advanced = true)
  override val zipFileRegex: String = CsvDataset.defaultZipFileRegex,
  @Param(label = "Delete file before workflow execution",
         value = "If set to true this will clear the specified file before executing a workflow that writes to it.",
         advanced = true)
  clearBeforeExecution: Boolean = false,
  @Param(label = "Trim whitespace and non-printable characters.",
         value = "If set to true, this will trim whitespace and non-printable characters from the contents of the CSV dataset.",
         advanced = true)
  override val trimWhitespaceAndNonPrintableCharacters: Boolean = false) extends Dataset
                                                                         with DatasetPluginAutoConfigurable[CsvDataset]
                                                                         with CsvDatasetTrait
                                                                         with TextBulkResourceBasedDataset {

  implicit val userContext: UserContext = UserContext.INTERNAL_USER

  override def mergeSchemata: Boolean = false

  override def createSource(resource: Resource): DataSource = {
    csvSource(resource)
  }

  override def linkSink(implicit userContext: UserContext): LinkSink = new CsvLinkSink(bulkWritableResource, csvSettings)

  override def entitySink(implicit userContext: UserContext): EntitySink = new CsvEntitySink(bulkWritableResource, csvSettings)

  private def csvSource(resource: Resource, ignoreMalformed: Boolean = false): CsvSource = resource match{
    case ror: ReadOnlyResource => csvSource(ror.resource, ignoreMalformed)
    case rkt: ResourceWithKnownTypes => new CsvSource(resource, csvSettings, properties, uri,
      regexFilter, ignoreBadLines = ignoreBadLines,
      ignoreMalformedInputExceptionInPropertyList = ignoreMalformed, specificTypeName = rkt.knownTypes.headOption
    )
    case _ => new CsvSource(resource, csvSettings, properties, uri,
      regexFilter, ignoreBadLines = ignoreBadLines,
      ignoreMalformedInputExceptionInPropertyList = ignoreMalformed
    )
  }

  /**
    * returns an auto-configured version of this plugin
    */
  override def autoConfigured(implicit pluginContext: PluginContext): CsvDataset = {
    useFirstResource { firstResource =>
      val source = csvSource(firstResource, ignoreMalformed = true)
      val autoConfig = source.autoConfigure()
      this.withParameters(ParameterValues(Map(
        "file" -> ParameterObjectValue(file),
        "separator" -> ParameterStringValue(if (autoConfig.detectedSeparator == "\t") "\\t" else autoConfig.detectedSeparator),
        "charset" -> ParameterStringValue(autoConfig.codecName),
        "linesToSkip" -> ParameterStringValue(autoConfig.linesToSkip.getOrElse(linesToSkip).toString)
      )))
    }
  }

  def resolveCsvQuote: String = {
    val quote: String = if (this.quote.equals("")) {
      "\u0000"
    }
    else {
      this.quote
    }
    quote
  }

  def characteristics: DatasetCharacteristics = CsvDataset.csvDataCharacteristics
}

object CsvDataset {

  final val defaultZipFileRegex = """^(?!.*[\/\\]\..*$|^\..*$).*\.csv"""

  /** Warning: Do NOT increase the default value here, it will request heap memory of this amount for every read operation of a column. */
  val DEFAULT_MAX_CHARS_PER_COLUMN = 128000

  final val csvDataCharacteristics = {
    DatasetCharacteristics(SupportedPathExpressions(
      specialPaths = Seq(
        SpecialPaths.IDX,
        SpecialPaths.LINE,
        SpecialPaths.COLUMN
      )),
      supportsMultipleTables = false,
      explicitSchema = true
    )
  }

  def fromSettings(settings: CsvSettings, file: WritableResource, ignoreBadLines: Boolean = false): CsvDataset = {
    new CsvDataset(
      file = file,
      separator = settings.separator.toString,
      arraySeparator = settings.arraySeparator.map(_.toString).getOrElse(""),
      quote = settings.quote.map(_.toString).getOrElse(""),
      maxCharsPerColumn = settings.maxCharsPerColumn.getOrElse(DEFAULT_MAX_CHARS_PER_COLUMN),
      quoteEscapeCharacter = settings.quoteEscapeChar.toString,
      ignoreBadLines = ignoreBadLines,
      linesToSkip = settings.linesToSkip,
      trimWhitespaceAndNonPrintableCharacters = settings.trimWhitespaceAndNonPrintableCharacters
    )
  }
}
