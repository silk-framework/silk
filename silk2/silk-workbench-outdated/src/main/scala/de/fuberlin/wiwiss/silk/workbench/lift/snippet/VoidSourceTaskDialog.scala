package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import de.fuberlin.wiwiss.silk.datasource.{Source, DataSource}
import de.fuberlin.wiwiss.silk.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.workbench.lift.util.{StringField, Dialog}
import scala.io.Source.fromInputStream
import java.net._
import de.fuberlin.wiwiss.silk.config.Dataset


/**
* A dialog to create and edit a source task.
*/
object VoidSourceTaskDialog extends Dialog {
 private val voidField = StringField("VoID URL", "The URL of the VoID file")

 private val nameField = StringField("Name", "The name of this source task", () => if (User().sourceTaskOpen) User().sourceTask.source.id else "")

 private val uriField = StringField("Endpoint URI", "The URI of the SPARQL endpoint", () => getParam("endpointURI"))

 private val graphField = StringField("Graph URI", "Only retrieve instances from a specific graph", () => getParam("graph"))

 private val retryCountField = StringField("Retry count", "To recover from intermittent SPARQL endpoint connection failures, " +
                  "the 'retryCount' parameter specifies the number of times to retry connecting.", () => getParam("retryCount"))

 private val retryPauseField = StringField("Retry pause", "To recover from intermittent SPARQL endpoint connection failures, " +
                  "the 'retryPause' parameter specifies how long to wait between retries.", () => getParam("retryPause"))

 override val fields = voidField /*:: nameField :: uriField :: graphField */ :: retryCountField :: retryPauseField :: Nil

 override def title = if (User().sourceTaskOpen) "Edit source task from VoID" else "Create source task from VoID"

 //Close the current task if the window is closed
 override protected def dialogParams = ("close" -> "closeTask") :: super.dialogParams

 override def onSubmit() = {
   val newSource = createSource()

   if (User().sourceTaskOpen && User().sourceTask.name != newSource.name) {
     val currentSource = User().sourceTask

     User().project.sourceModule.remove(currentSource.name)
     User().project.sourceModule.update(newSource)

     //Update all linking tasks to point to the updated task
     val linkingModule = User().project.linkingModule
     val updateFunc = new UpdateLinkingTask(currentSource.name, newSource.name)
     val updatedLinkingTasks = linkingModule.tasks.collect(updateFunc)
     for (linkingTask <- updatedLinkingTasks) {
       linkingModule.update(linkingTask)
     }
   } else {
     User().project.sourceModule.update(newSource)
   }

   Workspace.updateCmd
 }

 /**
  * Gets a parameter.
  */
 private def getParam(name: String): String = {
   if (User().sourceTaskOpen) {
     User().sourceTask.source.dataSource match {
       case DataSource(id, params) => params.get(name).getOrElse("")
     }
   } else {
     val sparqlEndpointDesc = DataSource.availablePlugins.find(_.id == "sparqlEndpoint").get
     val param = sparqlEndpointDesc.parameters.find(_.name == name).get

     param.defaultValue.flatMap(Option(_)).map(_.toString).getOrElse("")
   }
 }

 /**
  * Creates a new source task from the current dialog values.
  */
 private def createSource() = {
	var url = new URL("http://any23.org/any23/?format=ntriples&uri=" + URLEncoder.encode(voidField.value))
	val content = fromInputStream( url.openStream ).getLines.mkString("\n")

	println(content)
	val regexEndpoint = """(?s).*org/ns/void#sparqlEndpoint> <(http[^>]*).*""".r
	//  <http://open.vocab.org/terms/shortName> "lobid-resources"
	val regexName = """(?s).*http://open\.vocab\.org/terms/shortName> \"([^"]*).*""".r

	var sparqlEP = ""
	var dsName = ""
	
	content match {
		case regexEndpoint(sparql) => {
			sparqlEP = sparql
		}
		case _ => // Do nothing
	}	
	content match {
		case regexName(name) => {
			dsName = name
		}
		case _ => // Do nothing
	}	

	var params = Map("endpointURI" -> sparqlEP, "retryCount" -> retryCountField.value, "retryPause" -> retryPauseField.value)

	if (dsName == "") {
    //TODO
  }

	if (graphField.value != "") {
     params += ("graph" -> graphField.value)
   }
	
	 val source = Source(dsName, DataSource("sparqlEndpoint", params))
   SourceTask(source)
 }

 /**
  * Partial function which updates the source of a linking task.
  */
 private class UpdateLinkingTask(oldSource: Identifier, newSource: Identifier) extends PartialFunction[LinkingTask, LinkingTask] {
   override def isDefinedAt(task: LinkingTask) = {
     task.linkSpec.datasets.exists(_.sourceId == oldSource)
   }

   override def apply(task: LinkingTask) = {
     val updatedLinkSpec = task.linkSpec.copy(datasets = task.linkSpec.datasets.map(updateDataset))
     task.updateLinkSpec(updatedLinkSpec, User().project)
   }

   private def updateDataset(ds: Dataset) = {
      if (ds.sourceId == oldSource)
        ds.copy(sourceId = newSource)
      else
        ds
   }
 }

}