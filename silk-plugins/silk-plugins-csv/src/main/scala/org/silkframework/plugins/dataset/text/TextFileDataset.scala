package org.silkframework.plugins.dataset.text

import org.silkframework.dataset._
import org.silkframework.dataset.bulk.TextBulkResourceBasedDataset
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.plugins.dataset.charset.{CharsetAutocompletionProvider, CharsetUtils}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.{Resource, WritableResource}
import org.silkframework.util.{Identifier, Uri}

import scala.io.Codec

@Plugin(
  id = "text",
  label = "Text",
  categories = Array(DatasetCategories.file),
  description= "Reads and writes plain text files.",
  documentationFile = "TextFileDataset.md")
case class TextFileDataset(
   @Param("The plain text file. May also be a zip archive containing multiple text files.")
   file: WritableResource,
   @Param(value = "The file encoding, e.g., UTF-8, UTF-8-BOM, ISO-8859-1", autoCompletionProvider = classOf[CharsetAutocompletionProvider])
   charset: String = "UTF-8",
   @Param(value = "A type name that represents this file.", advanced = true)
   typeName: String = "document",
   @Param(value = "The single property that holds the text.", advanced = true)
   property: String = "text",
   @Param(label = "ZIP file regex", value = "If the input resource is a ZIP file, files inside the file are filtered via this regex.", advanced = true)
   override val zipFileRegex: String = ".*"
) extends Dataset with TextBulkResourceBasedDataset {

  override val codec: Codec = CharsetUtils.forName(charset)

  override def mimeType: Option[String] = Some("text/plain")

  val uri: Uri = DataSource.generateEntityUri(Identifier.fromAllowed(file.name), "text")

  val path: TypedPath = TypedPath(UntypedPath(property), ValueType.STRING, isAttribute = false)

  override def mergeSchemata: Boolean = false

  /**
    * Returns a data source for reading entities from the data set.
    */
  override def createSource(resource: Resource): DataSource = {
    new TextFileSource(this, resource)
  }

  /**
    * Returns a link sink for writing entity links to the data set.
    */
  override def linkSink(implicit userContext: UserContext): LinkSink = new TextFileSink(this)

  /**
    * Returns a entity sink for writing entities to the data set.
    */
  override def entitySink(implicit userContext: UserContext): EntitySink = new TextFileSink(this)

  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly(explicitSchema = true)
}
