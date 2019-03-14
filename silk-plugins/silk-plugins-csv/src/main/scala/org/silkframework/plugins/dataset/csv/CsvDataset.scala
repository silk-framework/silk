package org.silkframework.plugins.dataset.csv

import org.silkframework.dataset._
import org.silkframework.entity.{EntitySchema, TypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.resource._
import BulkResourceSupport._

@Plugin(
  id = "csv",
  label = "CSV file",
  description =
      """Retrieves all entities from a csv file."""
)
case class CsvDataset (
  @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
    file: WritableResource,
  @Param("Comma-separated list of URL-encoded properties. If not provided, the list of properties is read from the first line.")
    properties: String = "",
  @Param("The character that is used to separate values. If not provided, defaults to ',', i.e., comma-separated values. \"\\t\" for specifying tab-separated values, is also supported.")
    separator: String = ",",
  @Param(label = "Array separator", value = "The character that is used to separate the parts of array values. Write \"\\t\" to specify the tab character.")
    arraySeparator: String = "",
  @Param("Character used to quote values.")
    quote: String = "\"",
  @deprecated("This will be removed in the next release.", "")
  @Param("A pattern used to construct the entity URI. If not provided the prefix + the line number is used. An example of such a pattern is 'urn:zyx:{id}' where *id* is a name of a property.")
    uri: String = "",
  @Param("A regex filter used to match rows from the CSV file. If not set all the rows are used.")
    regexFilter: String = "",
  @Param("The file encoding, e.g., UTF8, ISO-8859-1")
    charset: String = "UTF-8",
  @Param("The number of lines to skip in the beginning, e.g. copyright, meta information etc.")
    linesToSkip: Int = 0,
  @Param("The maximum characters per column. If there are more characters found, the parser will fail.")
    maxCharsPerColumn: Int = CsvDataset.DEFAULT_MAX_CHARS_PER_COLUMN,
  @Param("If set to true then the parser will ignore lines that have syntax errors or do not have to correct number of fields according to the current config.")
    ignoreBadLines: Boolean = false,
  @Param(label = "Quote escape character",
    value = "Escape character to be used inside quotes, used to escape the quote character. It must also be used to escape itself, e.g. by doubling it, e.g. \"\". If left empty, it defaults to quote.")
  quoteEscapeCharacter: String = "\"") extends Dataset with DatasetPluginAutoConfigurable[CsvDataset] with WritableResourceDataset with CsvDatasetTrait with ResourceBasedDataset with BulkResourceSupport {


  def resource: WritableResource = checkIfBulkResource(file)

  override def source(implicit userContext: UserContext): DataSource = csvSource()

  override def linkSink(implicit userContext: UserContext): LinkSink = new CsvLinkSink(resource, csvSettings)

  override def entitySink(implicit userContext: UserContext): EntitySink = new CsvEntitySink(resource, csvSettings)

  private def csvSource(ignoreMalformed: Boolean = false) = new CsvSource(resource, csvSettings, properties, uri, regexFilter, codec,
    skipLinesBeginning = linesToSkip, ignoreBadLines = ignoreBadLines, ignoreMalformedInputExceptionInPropertyList = ignoreMalformed)

  /**
    * returns an auto-configured version of this plugin
    */
  override def autoConfigured(implicit userContext: UserContext): CsvDataset = {
    val source = csvSource(ignoreMalformed = true)
    val autoConfig = source.autoConfigure()
    this.copy(
      separator = if (autoConfig.detectedSeparator == "\t") "\\t" else autoConfig.detectedSeparator,
      charset = autoConfig.codecName,
      linesToSkip = autoConfig.linesToSkip.getOrElse(linesToSkip)
    )
  }

  override def replaceWritableResource(writableResource: WritableResource): WritableResourceDataset = {
    this.copy(file = writableResource)
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

  /**
    * Gets called when it is detected that all files in the bulk resource have the different schemata.
    * The implementing class needs to provide a bulk resource object with an input stream that
    * covers all files.
    * If that case cannot be supported None should be returned.
    *
    * @param bulkResource Bulk resource
    * @return
    */
  override def onMultiSchemaBulkContent(bulkResource: BulkResource): Option[BulkResource] = {
    throw new UnsupportedOperationException("The csv dataset does not support bulk resources with schema differences" +
      "in its sub resources")
  }

  /**
    * Gets called when it is detected that all files in the bulk resource have the same schema.
    * The implementing class needs to provide a logical concatenation of the individual resources.
    * If that case cannot be supported None should be returned.
    *
    * @param bulkResource Bulk resource
    * @return
    */
  override def onSingleSchemaBulkContent(bulkResource: BulkResource): Option[BulkResource] = {
    val combinedStream = BulkResourceSupport.combineStreams(bulkResource.inputStreams, Some(1))
    val copy = copyStream(combinedStream)
    Some(BulkResource.createBulkResourceWithStream(bulkResource, combinedStream))
  }

  /**
    * The implementing dataset must provide a way to determine the schema of each resource in the bulk resource.
    *
    * @param bulkResource bulk resource
    * @return Sequence of distinct schema objects
    */
  override def checkResourceSchema(bulkResource: BulkResource): Seq[EntitySchema] = {
    val csvSources = for (subResource <- bulkResource.subResources) yield {
      log info s"Checking schema for bulk resource contents: ${subResource.name}"
      new CsvSource(subResource, csvSettings, properties, uri, regexFilter, codec,
        skipLinesBeginning = 0, ignoreBadLines = ignoreBadLines, ignoreMalformedInputExceptionInPropertyList = false)
    }
    csvSources.map(s => {
      val properties = s.propertyList
      implicit val userContext: UserContext = UserContext.INTERNAL_USER
      val paths: IndexedSeq[TypedPath] = s.retrieveTypedPath("")
        .map(tp => TypedPath(tp.normalizedSerialization, tp.valueType))
      EntitySchema("", paths.toIndexedSeq)
    }).toIndexedSeq
  }

}

object CsvDataset {

  /** Warning: Do NOT increase the default value here, it will request heap memory of this amount for every read operation of a column. */
  val DEFAULT_MAX_CHARS_PER_COLUMN = 128000

  def fromSettings(settings: CsvSettings, file: WritableResource, ignoreBadLines: Boolean = false): CsvDataset = {
    new CsvDataset(
      file = file,
      separator = settings.separator.toString,
      arraySeparator = settings.arraySeparator.map(_.toString).getOrElse(""),
      quote = settings.quote.map(_.toString).getOrElse(""),
      maxCharsPerColumn = settings.maxCharsPerColumn.getOrElse(DEFAULT_MAX_CHARS_PER_COLUMN),
      quoteEscapeCharacter = settings.quoteEscapeChar.toString,
      ignoreBadLines = ignoreBadLines
    )
  }

}
