package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset._
import org.silkframework.dataset.bulk.BulkResourceBasedDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.MultilineStringParameter
import org.silkframework.runtime.resource.{Resource, WritableResource}
import org.silkframework.runtime.validation.ValidationException

import scala.util.{Failure, Success, Try}
import scala.xml._

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
                       @Param(value = "A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements", advanced = true)
                       uriPattern: String = "",
                       @Param(value = "The output template used for writing XML. Must be valid XML. The generated entity is identified through a processing instruction of the form <?MyEntity?>.")
                       outputTemplate: MultilineStringParameter = "<Root><?Entity?></Root>",
                       @Param(value = "Streaming allows for reading large XML files.", advanced = true)
                       streaming: Boolean = true,
                       @Param(label = "ZIP file regex", value = "If the input resource is a ZIP file, files inside the file are filtered via this regex.", advanced = true)
                       override val zipFileRegex: String = ".*\\.xml$") extends Dataset with BulkResourceBasedDataset {

  validateOutputTemplate()

  override def mergeSchemata: Boolean = true

  override def createSource(resource: Resource): DataSource = {
    if(streaming) {
      new XmlSourceStreaming(resource, basePath, uriPattern)
    }
    else {
      new XmlSourceInMemory(resource, basePath, uriPattern)
    }
  }

  override def linkSink(implicit userContext: UserContext): LinkSink = throw new NotImplementedError("Links cannot be written at the moment")

  override def entitySink(implicit userContext: UserContext): EntitySink = new XmlSink(file, outputTemplate.str)

  /**
    * Validates the output template parameter
    */
  private def validateOutputTemplate(): Unit = {
    val xml = loadString(outputTemplate.str)
    def collectProcInstructions(node: Node): Seq[ProcInstr] = {
      node match {
        case proc: ProcInstr => Seq(proc)
        case _ => node.child.flatMap(collectProcInstructions)
      }
    }
    val procInstructions = collectProcInstructions(xml)
    if (procInstructions.size != 1) {
      throw new ValidationException("outputTemplate must contain exactly one processing intruction of the form <?Entity?> to specify where the entities should be inserted.")
    }
  }

  private def loadString(templateString: String): Elem = {
    // Case 1: input <?Entity?>
    val case1 = Try(XML.loadString(s"<Root>$templateString</Root>"))
    // Case 2: input <Root><?Entity?></Root>
    val case1or2 = case1.orElse(Try(XML.loadString(templateString)))
    case1or2 match {
      case Success(elem) => elem
      case Failure(ex: SAXParseException) =>
        throw new ValidationException("outputTemplate could not be processed as valid XML. Error in line " + ex.getLineNumber + " column " + ex.getColumnNumber)
      case _ =>
        throw new ValidationException("outputTemplate must be valid XML containing a single processing instruction or a single processing " +
          "instruction of the form <?Entity?>!")
    }
  }

}
