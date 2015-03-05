package controllers.linking

import de.fuberlin.wiwiss.silk.execution
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import de.fuberlin.wiwiss.silk.learning.{LearningResult, LearningTask, LearningInput}
import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask
import models.{CurrentTaskStatusListener}
import models.linking._
import play.api.mvc.{Action, Controller}
import play.api.libs.json.{JsString, JsObject, JsArray, JsNumber, JsBoolean}
import de.fuberlin.wiwiss.silk.workspace.{Constants, Project, User}
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.entity.{SparqlRestriction, Path, Link}
import de.fuberlin.wiwiss.silk.runtime.oldtask.{TaskStatus, TaskFinished}
import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Aggregator, DistanceMeasure}
import de.fuberlin.wiwiss.silk.runtime.plugin.{Parameter, AnyPlugin}
import de.fuberlin.wiwiss.silk.util.Identifier._
import de.fuberlin.wiwiss.silk.config.{DatasetSelection, RuntimeConfig, LinkSpecification, Prefixes}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.util.{DPair, ValidationException, CollectLogs}
import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.util.ValidationException.ValidationError

object LinkingTaskApi extends Controller {

  private val log = Logger.getLogger(getClass.getName)

  def putLinkingTask(project: String, task: String) = Action { implicit request => {
    val values = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)

    val proj = User().workspace.project(project)
    implicit val prefixes = proj.config.prefixes

    val datasets = DPair(DatasetSelection(values("source"), Constants.SourceVariable, SparqlRestriction.fromSparql(Constants.SourceVariable, values("sourcerestriction"))),
      DatasetSelection(values("target"), Constants.TargetVariable, SparqlRestriction.fromSparql(Constants.TargetVariable, values("targetrestriction"))))

    proj.tasks[LinkingTask].find(_.name == task) match {
      //Update existing task
      case Some(oldTask) => {
        val updatedLinkSpec = oldTask.linkSpec.copy(datasets = datasets)
        val updatedLinkingTask = oldTask.updateLinkSpec(updatedLinkSpec, proj)
        proj.updateTask(updatedLinkingTask)
      }
      //Create new task
      case None => {
        val linkSpec =
          LinkSpecification(
            id = task,
            datasets = datasets,
            rule = LinkageRule(None),
            outputs = Nil
          )

        val linkingTask = LinkingTask(proj, linkSpec, ReferenceLinks(), updateCache = true)
        proj.updateTask(linkingTask)
      }
    }
    Ok
  }}

  def deleteLinkingTask(project: String, task: String) = Action {
    User().workspace.project(project).removeTask[LinkingTask](task)
    Ok
  }


  def getRule(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkingTask](taskName)
    implicit val prefixes = project.config.prefixes
    val ruleXml = task.linkSpec.rule.toXML

    Ok(ruleXml)
  }

  def putRule(projectName: String, taskName: String) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkingTask](taskName)
    implicit val prefixes = project.config.prefixes

    request.body.asXml match {
      case Some(xml) =>
        try {
          //Collect warnings while parsing linkage rule
          val warnings = CollectLogs(Level.WARNING, "de.fuberlin.wiwiss.silk.linkagerule") {
            //Load linkage rule
            val updatedRule = LinkageRule.load(project.resources)(prefixes)(xml.head)
            //Update linking task
            val updatedLinkSpec = task.linkSpec.copy(rule = updatedRule)
            val updatedTask = task.updateLinkSpec(updatedLinkSpec, project)
            project.updateTask(updatedTask)
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
    val task = project.task[LinkingTask](taskName)
    implicit val prefixes = project.config.prefixes
    val linkSpecXml = task.linkSpec.toXML

    Ok(linkSpecXml)
  }

  def putLinkSpec(projectName: String, taskName: String) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkingTask](taskName)
    val prefixes = project.config.prefixes

    request.body.asXml match {
      case Some(xml) => {
        try {
          //Collect warnings while parsing link spec
          val warnings = CollectLogs(Level.WARNING, "de.fuberlin.wiwiss.silk.linkspec") {
            //Load link specification
            val newLinkSpec = LinkSpecification.load(project.resources)(prefixes)(xml.head)

            //Update linking task
            val updatedTask = task.updateLinkSpec(newLinkSpec, project)
            project.updateTask(updatedTask)
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
    val task = project.task[LinkingTask](taskName)
    val referenceLinksXml = task.referenceLinks.toXML

    Ok(referenceLinksXml)
  }

  def putReferenceLinks(projectName: String, taskName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkingTask](taskName)

    for(data <- request.body.asMultipartFormData;
        file <- data.files) {
      val referenceLinks = ReferenceLinks.fromXML(scala.xml.XML.loadFile(file.ref.file))
      project.updateTask(task.updateReferenceLinks(referenceLinks, project))
    }
    Ok
  }}
  
  def putReferenceLink(projectName: String, taskName: String, linkType: String, source: String, target: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkingTask](taskName)
    val link = new Link(source, target)
    
    linkType match {
      case "positive" => {
        val updatedTask = task.updateReferenceLinks(task.referenceLinks.withPositive(link), project)
        project.updateTask(updatedTask)
      }
      case "negative" => {
        val updatedTask = task.updateReferenceLinks(task.referenceLinks.withNegative(link), project)
        project.updateTask(updatedTask)
      }
    }
    
    Ok
  }
  
  def deleteReferenceLink(projectName: String, taskName: String, source: String, target: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkingTask](taskName)
    val link = new Link(source, target)
    
    val updatedTask = task.updateReferenceLinks(task.referenceLinks.without(link), project)
    project.updateTask(updatedTask)
    
    Ok
  }

  def reloadLinkingCache(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkingTask](taskName)
    task.cache.reload(project, task)
    Ok
  }

  def startGenerateLinksTask(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.task[LinkingTask](taskName)

    //Retrieve parameters
    val params = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    val outputNames = params.get("outputs[]").toSeq.flatten
    val outputs = outputNames.map(name => project.task[DatasetTask](name).dataset)

    /** We use a custom runtime config */
    val runtimeConfig = RuntimeConfig(useFileCache = false, partitionSize = 300, generateLinksWithEntities = true)

    val generateLinksTask =
      execution.GenerateLinks.fromSources(
        inputs = project.tasks[DatasetTask].map(_.dataset),
        linkSpec = task.linkSpec,
        outputs = outputs,
        runtimeConfig = runtimeConfig
      )

    val taskControl = Activity.execute(generateLinksTask)
    CurrentGenerateLinksTask() = taskControl
    //TODO CurrentGeneratedLinks() = taskControl.value()

    Ok
  }

  def stopGenerateLinksTask(projectName: String, taskName: String) = Action {
    CurrentGenerateLinksTask().cancel()
    Ok
  }

  def writeReferenceLinks(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.task[LinkingTask](taskName)
    val params = request.body.asFormUrlEncoded.get

    for(posOutputName <- params.get("positiveOutput")) {
      val posOutput = project.task[DatasetTask](posOutputName.head).dataset.sink
      posOutput.writeLinks(task.referenceLinks.positive, params("positiveProperty").head)
    }

    for(negOutputName <- params.get("negativeOutput")) {
      val negOutput = project.task[DatasetTask](negOutputName.head).dataset.sink
      negOutput.writeLinks(task.referenceLinks.negative, params("negativeProperty").head)
    }

    Ok
  }

  def learningTask(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkingTask](taskName)

    //Start passive learning task
    val input =
      LearningInput(
        trainingEntities = task.cache.entities,
        seedLinkageRules = task.linkSpec.rule :: Nil
      )
    val learningTask = new LearningTask(input, CurrentConfiguration())
    CurrentLearningTask() = learningTask
    learningTask.runInBackground()
    Ok
  }

  def activeLearningTask(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkingTask](taskName)
    val datasets = project.tasks[DatasetTask].map(_.dataset)

    //Start active learning task
    val activeLearningTask =
      new ActiveLearningTask(
        config = CurrentConfiguration(),
        datasets = DPair.fromSeq(task.linkSpec.datasets.map(ds => datasets.find(_.id == ds.datasetId).get.source)),
        linkSpec = task.linkSpec,
        paths = task.cache.entityDescs.map(_.paths),
        referenceEntities = task.cache.entities,
        pool = CurrentPool(),
        population = CurrentPopulation()
      )
    CurrentValidationLinks() = Nil
    CurrentActiveLearningTask() = activeLearningTask
    activeLearningTask.runInBackground()
    Ok
  }

  // TODO
//  private val learningTaskListener = new CurrentTaskValueListener(CurrentLearningTask) {
//    override def onUpdate(result: LearningResult) {
//      CurrentPopulation() = result.population
//    }
//  }

  private val activeLearningTaskListener = new CurrentTaskStatusListener(CurrentActiveLearningTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskFinished => {
          CurrentPool() = task.pool
          CurrentPopulation() = task.population
          CurrentValidationLinks() = task.links
        }
        case _ =>
      }
    }
  }
}
