package controllers.workspace

import play.api.mvc.Action
import play.api.libs.json.{JsString, JsObject, JsArray, JsValue}
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.datasource.{DataSource, Source}
import de.fuberlin.wiwiss.silk.output.DataWriter
import play.api.mvc.Controller
import de.fuberlin.wiwiss.silk.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.modules.output.OutputTask
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.workspace.Constants
import de.fuberlin.wiwiss.silk.entity.SparqlRestriction
import de.fuberlin.wiwiss.silk.linkagerule.{LinkFilter, LinkageRule, TransformRule}
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import de.fuberlin.wiwiss.silk.workspace.io.SilkConfigImporter
import de.fuberlin.wiwiss.silk.config._
import java.io.{FileInputStream, ByteArrayOutputStream}
import de.fuberlin.wiwiss.silk.runtime.resource.EmptyResourceManager
import play.api.libs.iteratee.Enumerator
import scala.concurrent.ExecutionContext.Implicits.global
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask

object WorkspaceApi extends Controller {

  def newProject(project: String) = Action {
    User().workspace.createProject(project)
    Ok
  }

  def deleteProject(project: String) = Action {
    User().workspace.removeProject(project)
    Ok
  }

  def importProject(project: String) = Action { implicit request => {
    for(data <- request.body.asMultipartFormData;
        file <- data.files) {
      // Read the project from the received file
      val inputStream = new FileInputStream(file.ref.file)
      User().workspace.importProject(project, inputStream)
      inputStream.close()
    }
    Ok
  }}

  def exportProject(project: String) = Action {
    // Export the project into a byte array
    val outputStream = new ByteArrayOutputStream()
    User().workspace.exportProject(project, outputStream)
    val bytes = outputStream.toByteArray
    outputStream.close()

    Ok(bytes).as("application/zip").withHeaders("Content-Disposition" -> "attachment; filename=project.zip")
  }

  def importLinkSpec(projectName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)

    for(data <- request.body.asMultipartFormData;
        file <- data.files) {
      val config = LinkingConfig.fromXML(scala.xml.XML.loadFile(file.ref.file), new EmptyResourceManager())
      SilkConfigImporter(config, project)
    }
    Ok
  }}

  def updatePrefixes(project: String) = Action { implicit request => {
    val prefixMap = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)
    val projectObj = User().workspace.project(project)
    projectObj.config = projectObj.config.copy(prefixes = Prefixes(prefixMap))

    Ok
  }}

  def getResource(projectName: String, resourceName: String) = Action {
    val project = User().workspace.project(projectName)
    val resource = project.resources.get(resourceName)
    val enumerator = Enumerator.fromStream(resource.load)

    Ok.chunked(enumerator).withHeaders("Content-Disposition" -> "attachment")
  }

  def putResource(projectName: String, resourceName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)

    request.body.asMultipartFormData match {
      case Some(formData) if !formData.files.isEmpty =>
        try {
          val file = formData.files.head.ref.file
          val inputStream = new FileInputStream(file)
          project.resources.put(resourceName, inputStream)
          inputStream.close()
          Ok
        } catch {
          case ex: Exception => BadRequest(ex.getMessage)
        }
      case None => BadRequest("No resource provided.")
    }
  }}

  def deleteResource(projectName: String, resourceName: String) = Action {
    val project = User().workspace.project(projectName)
    project.resources.delete(resourceName)

    Ok
  }

  def getSource(projectName: String, sourceName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.sourceModule.task(sourceName)
    val sourceXml = task.source.toXML

    Ok(sourceXml)
  }

  def putSource(projectName: String, sourceName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    request.body.asXml match {
      case Some(xml) =>
        try {
          val sourceTask = SourceTask(project, Source.fromXML(xml.head, project.resources))
          project.sourceModule.update(sourceTask)
          Ok
        } catch {
          case ex: Exception => BadRequest(ex.getMessage)
        }
      case None => BadRequest("Expecting text/xml request body")
    }
  }}

  def deleteSource(project: String, source: String) = Action {
    User().workspace.project(project).sourceModule.remove(source)
    Ok
  }

  def getOutput(projectName: String, outputName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.outputModule.task(outputName)
    val outputXml = task.output.toXML

    Ok(outputXml)
  }

  def putOutput(projectName: String, output: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    request.body.asXml match {
      case Some(xml) => {
        try {
          val outputTask = OutputTask(Output.fromXML(xml.head, project.resources))
          project.outputModule.update(outputTask)
          Ok
        } catch {
          case ex: Exception => BadRequest(ex.getMessage)
        }
      }
      case None => BadRequest("Expecting text/xml request body")
    }
  }}

  def deleteOutput(project: String, output: String) = Action {
    User().workspace.project(project).outputModule.remove(output)
    Ok
  }
}