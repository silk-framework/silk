package controllers.linking

import java.util.logging.{Level, Logger}

import controllers.util.ProjectUtils._
import models.JsonError
import org.silkframework.config.{DatasetSelection, LinkSpecification}
import org.silkframework.dataset.Dataset
import org.silkframework.entity.{Link, Restriction}
import org.silkframework.evaluation.ReferenceLinks
import org.silkframework.execution.{GenerateLinks => GenerateLinksActivity}
import org.silkframework.learning.LearningActivity
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.rule.LinkageRule
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.validation.{ValidationError, ValidationException, ValidationWarning}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.util.Identifier._
import org.silkframework.util.{CollectLogs, DPair, Identifier, Uri}
import org.silkframework.workspace.activity.linking.{LinkingPathsCache, ReferenceEntitiesCache}
import org.silkframework.workspace.{Project, User}
import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.mvc.{Action, AnyContentAsXml, Controller}

object LinkingTaskApi extends Controller {

  private val log = Logger.getLogger(getClass.getName)

  def getLinkingTask(projectName: String, taskName: String) = Action {
    val project: Project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val xml = XmlSerialization.toXml(task.data)
    Ok(xml)
  }

  def putLinkingTask(project: String, task: String) = Action { implicit request => {
    val values = request.body.asFormUrlEncoded.getOrElse(request.queryString).mapValues(_.head)

    val proj: Project = User().workspace.project(project)
    implicit val prefixes = proj.config.prefixes

    val datasets =
      DPair(DatasetSelection(values("source"), Uri.parse(values.getOrElse("sourceType", ""), prefixes), Restriction.custom(values.getOrElse("sourceRestriction", ""))),
            DatasetSelection(values("target"), Uri.parse(values.getOrElse("targetType", ""), prefixes), Restriction.custom(values.getOrElse("targetRestriction", ""))))
    val outputs = values.get("output").filter(_.nonEmpty).map(Identifier(_)).toSeq

    proj.tasks[LinkSpecification].find(_.name == task) match {
      //Update existing task
      case Some(oldTask) => {
        val updatedLinkSpec = oldTask.data.copy(dataSelections = datasets, outputs = outputs)
        proj.updateTask(task, updatedLinkSpec)
      }
      //Create new task
      case None => {
        val linkSpec =
          LinkSpecification(
            id = task,
            dataSelections = datasets,
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
    val ruleXml = XmlSerialization.toXml(task.data.rule)

    Ok(ruleXml)
  }

  def putRule(projectName: String, taskName: String) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    implicit val readContext = ReadContext(resources, prefixes)

    request.body.asXml match {
      case Some(xml) =>
        try {
          //Collect warnings while parsing linkage rule
          val warnings = CollectLogs(Level.WARNING, "org.silkframework.linkagerule") {
            //Load linkage rule
            val updatedRule = XmlSerialization.fromXml[LinkageRule](xml.head)
            //Update linking task
            val updatedLinkSpec = task.data.copy(rule = updatedRule)
            project.updateTask(taskName, updatedLinkSpec)
          }
          // Return warnings
          Ok(JsonError("Linkage rule committed successfully", issues = warnings.map(log => ValidationWarning(log.getMessage))))
        } catch {
          case ex: ValidationException =>
            log.log(Level.INFO, "Invalid linkage rule")
            BadRequest(JsonError("Invalid linkage rule", issues = ex.errors))
          case ex: Exception =>
            log.log(Level.INFO, "Failed to commit linkage rule", ex)
            InternalServerError(JsonError("Failed to commit linkage rule", issues = ValidationError("Error in back end: " + ex.getMessage) :: Nil))
        }
      case None =>
        BadRequest(JsonError("Expecting text/xml request body"))
    }
  }}

  def getLinkSpec(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    implicit val prefixes = project.config.prefixes
    val linkSpecXml = XmlSerialization.toXml(task.data)

    Ok(linkSpecXml)
  }

  def putLinkSpec(projectName: String, taskName: String) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    implicit val readContext = ReadContext(resources, prefixes)

    request.body.asXml match {
      case Some(xml) => {
        try {
          //Collect warnings while parsing link spec
          val warnings = CollectLogs(Level.WARNING, "org.silkframework.linkspec") {
            //Load link specification
            val newLinkSpec = XmlSerialization.fromXml[LinkSpecification](xml.head)
            //Update linking task
            project.updateTask(taskName, newLinkSpec.copy(referenceLinks = task.data.referenceLinks))
          }

          Ok(JsonError("Linkage rule committed successfully", issues = warnings.map(log => ValidationWarning(log.getMessage))))
        } catch {
          case ex: ValidationException =>
            log.log(Level.INFO, "Invalid linkage rule")
            BadRequest(JsonError("Invalid linkage rule", issues = ex.errors))
          case ex: Exception =>
            log.log(Level.INFO, "Failed to commit linkage rule", ex)
            InternalServerError(JsonError("Failed to commit linkage rule", issues = ValidationError("Error in back end: " + ex.getMessage) :: Nil))
        }
      }
      case None => BadRequest(JsonError("Expecting text/xml request body"))
    }
  }}

  def getReferenceLinks(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val referenceLinksXml = task.data.referenceLinks.toXML

    Ok(referenceLinksXml).withHeaders("Content-Disposition" -> s"attachment; filename=referenceLinks.xml")
  }

  def putReferenceLinks(projectName: String, taskName: String, generateNegative: Boolean) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)

    for(data <- request.body.asMultipartFormData;
        file <- data.files) {
      var referenceLinks = ReferenceLinks.fromXML(scala.xml.XML.loadFile(file.ref.file))
      if(generateNegative) {
        referenceLinks = referenceLinks.generateNegative
      }
      project.updateTask(taskName, task.data.copy(referenceLinks = referenceLinks))
    }
    Ok
  }}

  /**
   * Delete all reference links of specific types.
 *
   * @param projectName
   * @param taskName
   * @param positive if true
   * @param negative
   * @param unlabeled
   * @return
   */
  def deleteReferenceLinks(projectName: String, taskName: String, positive: Boolean, negative: Boolean, unlabeled: Boolean) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val referenceLinks = task.data.referenceLinks

    val newReferenceLinks =
      ReferenceLinks(
        positive = if(positive) Set.empty else referenceLinks.positive,
        negative = if(negative) Set.empty else referenceLinks.negative,
        unlabeled = if(unlabeled) Set.empty else referenceLinks.unlabeled
      )
    task.update(task.data.copy(referenceLinks = newReferenceLinks))

    Ok
  }}


  /**
   * Add a reference link to a specific linking task.
 *
   * @param projectName
   * @param taskName
   * @param linkType E.g. "negative" or "positive"
   * @param source the source entity URI
   * @param target the target entity URI
   * @return
   */
  def putReferenceLink(projectName: String, taskName: String, linkType: String, source: String, target: String) = Action {
    log.info(s"Adding $linkType reference link: $source - $target")
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

  /**
   * Delete a reference link
 *
   * @param projectName
   * @param taskName
   * @param source source URI
   * @param target target URI
   * @return
   */
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
    val referenceEntitiesCache = task.activity[ReferenceEntitiesCache].control
    referenceEntitiesCache.reset()
    referenceEntitiesCache.start()
    Ok
  }

  def startGenerateLinksTask(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val generateLinksActivity = task.activity[GenerateLinksActivity].control
    generateLinksActivity.start()
    Ok
  }

  def stopGenerateLinksTask(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val generateLinksActivity = task.activity[GenerateLinksActivity].control
    generateLinksActivity.cancel()
    Ok
  }

  def writeReferenceLinks(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val params = request.body.asFormUrlEncoded.get

    for(posOutputName <- params.get("positiveOutput")) {
      val posOutput = project.task[Dataset](posOutputName.head).data.linkSink
      posOutput.writeLinks(task.data.referenceLinks.positive, params("positiveProperty").head)
    }

    for(negOutputName <- params.get("negativeOutput")) {
      val negOutput = project.task[Dataset](negOutputName.head).data.linkSink
      negOutput.writeLinks(task.data.referenceLinks.negative, params("negativeProperty").head)
    }

    Ok
  }

  def learningActivity(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    task.activity[LearningActivity].control.start()
    Ok
  }

  def activeLearningActivity(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val learningActivity = task.activity[ActiveLearning]

    if(task.data.referenceLinks.unlabeled.isEmpty) {
      val updatedReferenceLinks = task.data.referenceLinks.copy(unlabeled = learningActivity.value.pool.links.toSet)
      task.update(task.data.copy(referenceLinks = updatedReferenceLinks))
    }

    learningActivity.control.start()
    Ok
  }

  // Get the project and linking task
  private def projectAndTask(projectName: String, taskName: String)  = {
    getProjectAndTask[LinkSpecification](projectName, taskName)
  }

  def postLinkDatasource(projectName: String, taskName: String) = Action { request =>
    request.body match {
      case AnyContentAsXml(xmlRoot) =>
        try{
          val (_, task) = projectAndTask(projectName, taskName)
          implicit val resourceManager = createInmemoryResourceManagerForResources(xmlRoot)
          val linkSource = createDataSource(xmlRoot, Some("sourceDataset"))
          val linkTarget = createDataSource(xmlRoot, Some("targetDataset"))
          val (model, linkSink) = createLinkSink(xmlRoot)
          val link = new GenerateLinksActivity(DPair(linkSource, linkTarget), task.data, Seq(linkSink))
          Activity(link).startBlocking()
          val acceptedContentType = request.acceptedTypes.headOption.map(_.mediaType).getOrElse("application/n-triples")
          result(model, acceptedContentType, "Successfully generated links")
        } catch {
          case e: NoSuchElementException =>
            NotFound
        }
      case _ =>
        UnsupportedMediaType("Only XML supported")
    }
  }
}