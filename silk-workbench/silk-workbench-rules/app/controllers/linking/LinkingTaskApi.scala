package controllers.linking

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.config.{DatasetSelection, LinkSpecification, RuntimeConfig}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.{Link, SparqlRestriction}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import de.fuberlin.wiwiss.silk.execution.{GenerateLinks => GenerateLinksActivity}
import de.fuberlin.wiwiss.silk.learning.active.{ActiveLearning}
import de.fuberlin.wiwiss.silk.learning.{LearningInput, LearningActivity}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.util.Identifier._
import de.fuberlin.wiwiss.silk.util.ValidationException.ValidationError
import de.fuberlin.wiwiss.silk.util.{CollectLogs, DPair, ValidationException}
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingCaches
import de.fuberlin.wiwiss.silk.workspace.{Project, Constants, User}
import models.CurrentActivityStatusListener
import models.linking._
import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.mvc.{Action, Controller}

object LinkingTaskApi extends Controller {

  private val log = Logger.getLogger(getClass.getName)

  def putLinkingTask(project: String, task: String) = Action { implicit request => {
    val values = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)

    val proj: Project = User().workspace.project(project)
    implicit val prefixes = proj.config.prefixes

    val datasets =
      DPair(DatasetSelection(values("source"), Constants.SourceVariable, SparqlRestriction.fromSparql(Constants.SourceVariable, values("sourcerestriction"))),
            DatasetSelection(values("target"), Constants.TargetVariable, SparqlRestriction.fromSparql(Constants.TargetVariable, values("targetrestriction"))))
    val outputs = values.get("output").filter(_.nonEmpty).map(proj.task[Dataset](_).data).toSeq

    proj.tasks[LinkSpecification].find(_.name == task) match {
      //Update existing task
      case Some(oldTask) => {
        val updatedLinkSpec = oldTask.data.copy(datasets = datasets, outputs = outputs)
        proj.updateTask(task, updatedLinkSpec)
      }
      //Create new task
      case None => {
        val linkSpec =
          LinkSpecification(
            id = task,
            datasets = datasets,
            rule = LinkageRule(None),
            outputs = outputs
          )

        proj.updateTask(task, linkSpec)
      }
    }
    Ok
  }}

  def deleteLinkingTask(project: String, task: String) = Action {
    User().workspace.project(project).removeTask[LinkSpecification](task)
    Ok
  }


  def getRule(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    implicit val prefixes = project.config.prefixes
    val ruleXml = task.data.rule.toXML

    Ok(ruleXml)
  }

  def putRule(projectName: String, taskName: String) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    implicit val prefixes = project.config.prefixes

    request.body.asXml match {
      case Some(xml) =>
        try {
          //Collect warnings while parsing linkage rule
          val warnings = CollectLogs(Level.WARNING, "de.fuberlin.wiwiss.silk.linkagerule") {
            //Load linkage rule
            val updatedRule = LinkageRule.load(project.resources)(prefixes)(xml.head)
            //Update linking task
            val updatedLinkSpec = task.data.copy(rule = updatedRule)
            project.updateTask(taskName, updatedLinkSpec)
          }
          // Return warnings
          Ok(statusJson(warnings = warnings.map(_.getMessage)))
        } catch {
          case ex: ValidationException =>
            log.log(Level.INFO, "Invalid linkage rule")
            BadRequest(statusJson(errors = ex.errors))
          case ex: Exception =>
            log.log(Level.INFO, "Failed to save linkage rule", ex)
            InternalServerError(statusJson(errors = ValidationError("Error in back end: " + ex.getMessage) :: Nil))
        }
      case None =>
        BadRequest("Expecting text/xml request body")
    }
  }}

  def getLinkSpec(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    implicit val prefixes = project.config.prefixes
    val linkSpecXml = task.data.toXML

    Ok(linkSpecXml)
  }

  def putLinkSpec(projectName: String, taskName: String) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val prefixes = project.config.prefixes

    request.body.asXml match {
      case Some(xml) => {
        try {
          //Collect warnings while parsing link spec
          val warnings = CollectLogs(Level.WARNING, "de.fuberlin.wiwiss.silk.linkspec") {
            //Load link specification
            val newLinkSpec = LinkSpecification.load(project.resources)(prefixes)(xml.head)
            //Update linking task
            project.updateTask(taskName, newLinkSpec.copy(referenceLinks = task.data.referenceLinks))
          }

          Ok(statusJson(warnings = warnings.map(_.getMessage)))
        } catch {
          case ex: ValidationException => {
            log.log(Level.INFO, "Invalid linkage rule")
            BadRequest(statusJson(errors = ex.errors))
          }
          case ex: Exception => {
            log.log(Level.INFO, "Failed to save linkage rule", ex)
            InternalServerError(statusJson(errors = ValidationError("Error in back end: " + ex.getMessage) :: Nil))
          }
        }
      }
      case None => BadRequest("Expecting text/xml request body")
    }
  }}

  private def statusJson(errors: Seq[ValidationError] = Nil, warnings: Seq[String] = Nil, infos: Seq[String] = Nil) = {
    /**Generates a Json expression from an error */
    def errorToJsExp(error: ValidationError) = JsObject(("message", JsString(error.toString)) :: ("id", JsString(error.id.map(_.toString).getOrElse(""))) :: Nil)

    JsObject(
      ("error", JsArray(errors.map(errorToJsExp))) ::
      ("warning", JsArray(warnings.map(JsString(_)))) ::
      ("info", JsArray(infos.map(JsString(_)))) :: Nil
    )
  }

  def getReferenceLinks(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val referenceLinksXml = task.data.referenceLinks.toXML

    Ok(referenceLinksXml)
  }

  def putReferenceLinks(projectName: String, taskName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)

    for(data <- request.body.asMultipartFormData;
        file <- data.files) {
      val referenceLinks = ReferenceLinks.fromXML(scala.xml.XML.loadFile(file.ref.file))
      project.updateTask(taskName, task.data.copy(referenceLinks = referenceLinks))
    }
    Ok
  }}
  
  def putReferenceLink(projectName: String, taskName: String, linkType: String, source: String, target: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val link = new Link(source, target)
    
    linkType match {
      case "positive" => {
        val updatedRefLinks = task.data.copy(referenceLinks = task.data.referenceLinks.withPositive(link))
        project.updateTask(taskName, updatedRefLinks)
      }
      case "negative" => {
        val updatedRefLinks = task.data.copy(referenceLinks = task.data.referenceLinks.withNegative(link))
        project.updateTask(taskName, updatedRefLinks)
      }
    }
    
    Ok
  }
  
  def deleteReferenceLink(projectName: String, taskName: String, source: String, target: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val link = new Link(source, target)
    
    val updatedTask = task.data.copy(referenceLinks = task.data.referenceLinks.without(link))
    project.updateTask(taskName, updatedTask)
    
    Ok
  }

  def reloadLinkingCache(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    task.cache[LinkingCaches].reload(project, task.data)
    Ok
  }

  def startGenerateLinksTask(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val generateLinksActivity = task.activity[GenerateLinksActivity]
    generateLinksActivity.start()
    Ok
  }

  def stopGenerateLinksTask(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val generateLinksActivity = task.activity[GenerateLinksActivity]
    generateLinksActivity.cancel()
    Ok
  }

  def writeReferenceLinks(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val params = request.body.asFormUrlEncoded.get

    for(posOutputName <- params.get("positiveOutput")) {
      val posOutput = project.task[Dataset](posOutputName.head).data.sink
      posOutput.writeLinks(task.data.referenceLinks.positive, params("positiveProperty").head)
    }

    for(negOutputName <- params.get("negativeOutput")) {
      val negOutput = project.task[Dataset](negOutputName.head).data.sink
      negOutput.writeLinks(task.data.referenceLinks.negative, params("negativeProperty").head)
    }

    Ok
  }

  def learningActivity(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    task.activity[LearningActivity].start()
    Ok
  }

  def activeLearningActivity(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    task.activity[ActiveLearning].start()
    Ok
  }
}
