package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DatasetCharacteristics.{SpecialPathInfo, SuggestedForEnum, SupportedPathExpressions}
import org.silkframework.dataset._
import org.silkframework.dataset.bulk.BulkResourceBasedDataset
import org.silkframework.plugins.dataset.hierarchical.HierarchicalSink.DEFAULT_MAX_SIZE
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.XmlCodeParameter
import org.silkframework.runtime.resource.{Resource, WritableResource}

@Plugin(
  id = "xml",
  label = "XML",
  categories = Array(DatasetCategories.file),
  description = "Read from or write to an XML file.",
  documentationFile = "XmlDatasetDocumentation.md"
)
case class XmlDataset( @Param("The XML file. This may also be a zip archive of multiple XML files that share the same schema.")
                       file: WritableResource,
                       @Param(value = "The base path when writing XML. For instance: /RootElement/Entity. Should no longer be used for reading XML! Instead, set the base path by specifying it as input type on the subsequent transformation or linking tasks.", advanced = true)
                       basePath: String = "",
                       @deprecated("This will be removed in the next release.", "")
                       @Param(label = "URI pattern", value = "A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements", advanced = true)
                       uriPattern: String = "",
                       @Param(value = "The output template used for writing XML. Must be valid XML. The generated entity is identified through a processing instruction of the form <?MyEntity?>.", advanced = true)
                       outputTemplate: XmlCodeParameter = XmlCodeParameter("<Root><?Entity?></Root>"),
                       @Param(value = "Streaming allows for reading large XML files.", advanced = true)
                       streaming: Boolean = true,
                       @Param(value = "Maximum depth of written XML. This acts as a safe guard if a recursive structure is written.", advanced = true)
                       maxDepth: Int = DEFAULT_MAX_SIZE,
                       @Param(label = "ZIP file regex", value = "If the input resource is a ZIP file, files inside the file are filtered via this regex.", advanced = true)
                       override val zipFileRegex: String = ".*\\.xml$") extends Dataset with BulkResourceBasedDataset {

  // Parse and validate the output template
  private val parsedOutputTemplate = XmlOutputTemplate.parse(outputTemplate.str)

  override def mergeSchemata: Boolean = true

  override def createSource(resource: Resource): DataSource = {
    if(streaming) {
      new XmlSourceStreaming(resource, basePath, uriPattern)
    }
    else {
      resource.checkSizeForInMemory()
      new XmlSourceInMemory(resource, basePath, uriPattern)
    }
  }

  override def linkSink(implicit userContext: UserContext): LinkSink = new TableLinkSink(new XmlSink(file, parsedOutputTemplate, maxDepth))

  override def entitySink(implicit userContext: UserContext): EntitySink = new XmlSink(file, parsedOutputTemplate, maxDepth)

  override def characteristics: DatasetCharacteristics = XmlDataset.characteristics
}

object XmlDataset {

  object SpecialXmlPaths {
    final val ID = "#id"
    final val TAG = "#tag"
    final val TEXT = "#text"
    final val ALL_CHILDREN = "*"
    final val ALL_CHILDREN_RECURSIVE = "**"
    final val BACKWARD_PATH = "\\.."
  }
  import SpecialXmlPaths._
  final val characteristics = DatasetCharacteristics(
    SupportedPathExpressions(
      multiHopPaths = true,
      propertyFilter = true,
      specialPaths = Seq(
        SpecialPathInfo(BACKWARD_PATH, Some("Navigate to parent element.")),
        SpecialPathInfo(ID, Some("A document-wide unique ID of the entity."), SuggestedForEnum.ValuePathOnly),
        SpecialPathInfo(TAG, Some("The element tag of the entity."), SuggestedForEnum.ValuePathOnly),
        SpecialPathInfo(TEXT, Some("The concatenated text inside an element."), SuggestedForEnum.ValuePathOnly),
        SpecialPathInfo(ALL_CHILDREN, Some("Selects all direct children of the entity.")),
        SpecialPathInfo(ALL_CHILDREN_RECURSIVE, Some("Selects all children nested below the entity at any depth."))
      )
    )
  )

}
