package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import java.io.ByteArrayInputStream
import io.Source
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.{SHtml, FileParamHolder}
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.OnLoad
import de.fuberlin.wiwiss.silk.linkspec.DatasetSpecification
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.instance.{Path, InstanceSpecification}
import de.fuberlin.wiwiss.silk.evaluation.{Alignment, AlignmentReader}

class ImportReferenceLinksDialog {

  def render(xhtml: NodeSeq): NodeSeq = {
    var fileHolder: FileParamHolder = null

    bind("entry", xhtml,
         "file" -> SHtml.fileUpload(fileHolder = _),
         "submit" -> SHtml.submit("Open", () => loadFromFile(fileHolder), "style" -> "float:right;"))
  }

  private def loadFromFile(fileHolder: FileParamHolder) = fileHolder match {
    case FileParamHolder(_, _, fileName, data) => {
      val alignment = fileName.split('.').last match {
        case "xml" | "rdf" => AlignmentReader.readAlignment(new ByteArrayInputStream(data))
        case "nt" => AlignmentReader.readNTriples(Source.fromBytes(data))
        case ext => throw new IllegalArgumentException("Unsupported file extension: '" + ext + "'.")
      }

      updateAlignment(alignment)
    }
    case _ =>
  }

  private def loadFromSources()  {
    val datasets = User().linkingTask.linkSpec.datasets

    val sourceInstances = getInstances(datasets.source).toList
    val targetInstances = getInstances(datasets.target).toList

    val sourceUris = sourceInstances.map(_.uri).toSet
    val targetUris = targetInstances.map(_.uri).toSet

    val sourceLinks = sourceInstances.flatMap(i => i.evaluate(0).map(new Link(i.uri, _)))
                                     .filter(link => targetUris.contains(link.target))

    val targetLinks = targetInstances.flatMap(i => i.evaluate(0).map(new Link(_, i.uri)))
                                     .filter(link => sourceUris.contains(link.source))

    val links = sourceLinks ++ targetLinks

    updateAlignment(Alignment(links.toSet))
  }

  private def getInstances(dataset: DatasetSpecification, uris: Seq[String] = Seq.empty) = {
    val source = User().project.sourceModule.task(dataset.sourceId).source
    val instanceSpec =
      InstanceSpecification(
        variable = dataset.variable,
        restrictions = dataset.restriction,
        paths = Path.parse("?" + dataset.variable + "/<http://www.w3.org/2002/07/owl#sameAs>") :: Nil
      )

    source.retrieve(instanceSpec, uris)
  }

  private def updateAlignment(alignment: Alignment) {
    //If the alignment does not define any negative links -> generate some
    if (alignment.negative.isEmpty) {
      val updatedLinkingTask = User().linkingTask.updateAlignment(alignment.generateNegative, User().project)

      User().project.linkingModule.update(updatedLinkingTask)
      User().task = updatedLinkingTask
    }
    else {
      val updatedLinkingTask = User().linkingTask.updateAlignment(alignment, User().project)
      User().project.linkingModule.update(updatedLinkingTask)
      User().task = updatedLinkingTask
    }
  }
}

object ImportReferenceLinksDialog {
  def initCmd = OnLoad(JsRaw("$('#importReferenceLinksDialog').dialog({ autoOpen: false, width: 500, modal: true })").cmd)

  def openCmd = JsRaw("$('#importReferenceLinksDialog').dialog('open')").cmd
}