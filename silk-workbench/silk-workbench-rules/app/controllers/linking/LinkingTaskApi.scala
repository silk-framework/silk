package controllers.linking

import java.util.logging.{Level, Logger}

import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.util.ProjectUtils._
import controllers.util.SerializationUtils
import javax.inject.Inject
import org.silkframework.config.MetaData
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.{Entity, Link, Restriction}
import org.silkframework.learning.LearningActivity
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.rule.evaluation.ReferenceLinks
import org.silkframework.rule.execution.{GenerateLinks => GenerateLinksActivity}
import org.silkframework.rule.{DatasetSelection, LinkSpec, LinkageRule, RuntimeLinkingConfig}
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlSerialization}
import org.silkframework.runtime.validation._
import org.silkframework.serialization.json.LinkingSerializers.LinkJsonFormat
import org.silkframework.util.Identifier._
import org.silkframework.util.{CollectLogs, DPair, Identifier, Uri}
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workspace.activity.linking.{EvaluateLinkingActivity, ReferenceEntitiesCache}
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{Action, AnyContent, AnyContentAsXml, InjectedController}
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._

class LinkingTaskApi @Inject() () extends InjectedController {

  private val log = Logger.getLogger(getClass.getName)

  def getLinkingTask(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project: Project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val xml = XmlSerialization.toXml(task.data)
    Ok(xml)
  }

  def pushLinkingTask(project: String, task: String, createOnly: Boolean): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val values = request.body.asFormUrlEncoded.getOrElse(request.queryString).mapValues(_.head)

    val proj: Project = WorkspaceFactory().workspace.project(project)
    implicit val prefixes = proj.config.prefixes

    val datasets =
      DPair(DatasetSelection(values("source"), Uri.parse(values.getOrElse("sourceType", ""), prefixes), Restriction.custom(values.getOrElse("sourceRestriction", ""))),
            DatasetSelection(values("target"), Uri.parse(values.getOrElse("targetType", ""), prefixes), Restriction.custom(values.getOrElse("targetRestriction", ""))))
    val outputs = values.get("output").filter(_.nonEmpty).map(str => str.split(",").map(Identifier(_))).toSeq.flatten

    proj.tasks[LinkSpec].find(_.id == task) match {
      //Update existing task
      case Some(oldTask) if !createOnly => {
        val updatedLinkSpec = oldTask.data.copy(dataSelections = datasets, outputs = outputs)
        proj.updateTask(task, updatedLinkSpec)
      }
      //Create new task
      case _ => {
        val linkSpec =
          LinkSpec(
            dataSelections = datasets,
            rule = LinkageRule(None),
            outputs = outputs
          )

        proj.addTask(task, linkSpec, MetaData(MetaData.labelFromId(task)))
      }
    }
    Ok
  }

  def deleteLinkingTask(project: String, task: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    WorkspaceFactory().workspace.project(project).removeTask[LinkSpec](task)
    Ok
  }


  def getRule(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    implicit val prefixes = project.config.prefixes
    val ruleXml = XmlSerialization.toXml(task.data.rule)

    Ok(ruleXml)
  }

  def putRule(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    implicit val readContext = ReadContext(resources, prefixes)

    request.body.asXml match {
      case Some(xml) =>
        try {
          //Collect warnings while parsing linkage rule
          val warnings = CollectLogs(Level.WARNING, classOf[LinkageRule].getPackage.getName) {
            //Load linkage rule
            val updatedRule = XmlSerialization.fromXml[LinkageRule](xml.head)
            //Update linking task
            val updatedLinkSpec = task.data.copy(rule = updatedRule)
            project.updateTask(taskName, updatedLinkSpec)
          }
          // Return warnings
          ErrorResult.validation(OK, "Linkage rule committed successfully", issues = warnings.map(log => ValidationWarning(log.getMessage)))
        } catch {
          case ex: ValidationException =>
            log.log(Level.INFO, "Invalid linkage rule")
            ErrorResult.validation(BAD_REQUEST, "Invalid linkage rule", issues = ex.errors)
          case ex: Exception =>
            log.log(Level.INFO, "Failed to commit linkage rule", ex)
            ErrorResult.validation(INTERNAL_SERVER_ERROR, "Failed to commit linkage rule", issues = ValidationError("Error in back end: " + ex.getMessage) :: Nil)
        }
      case None =>
        ErrorResult(BadUserInputException("Expecting text/xml request body"))
    }
  }

  def getLinkSpec(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    implicit val prefixes = project.config.prefixes
    val linkSpecXml = XmlSerialization.toXml(task.data)

    Ok(linkSpecXml)
  }

  def putLinkSpec(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    implicit val readContext = ReadContext(resources, prefixes)

    request.body.asXml match {
      case Some(xml) => {
        try {
          //Collect warnings while parsing link spec
          val warnings = CollectLogs(Level.WARNING, "org.silkframework.linkspec") {
            //Load link specification
            val newLinkSpec = XmlSerialization.fromXml[LinkSpec](xml.head)
            //Update linking task
            project.updateTask(taskName, newLinkSpec.copy(referenceLinks = task.data.referenceLinks))
          }

          ErrorResult.validation(OK, "Linkage rule committed successfully", issues = warnings.map(log => ValidationWarning(log.getMessage)))
        } catch {
          case ex: ValidationException =>
            log.log(Level.INFO, "Invalid linkage rule")
            ErrorResult.validation(BAD_REQUEST, "Invalid linkage rule", issues = ex.errors)
          case ex: Exception =>
            log.log(Level.INFO, "Failed to commit linkage rule", ex)
            ErrorResult.validation(INTERNAL_SERVER_ERROR, "Failed to commit linkage rule", issues = ValidationError("Error in back end: " + ex.getMessage) :: Nil)
        }
      }
      case None => ErrorResult(BadUserInputException("Expecting text/xml request body"))
    }
  }

  def getReferenceLinks(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val referenceLinksXml = task.data.referenceLinks.toXML

    Ok(referenceLinksXml).withHeaders("Content-Disposition" -> s"attachment; filename=referenceLinks.xml")
  }

  def putReferenceLinks(projectName: String, taskName: String, generateNegative: Boolean): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)

    for(data <- request.body.asMultipartFormData;
        file <- data.files) {
      var referenceLinks = ReferenceLinks.fromXML(scala.xml.XML.loadFile(file.ref.path.toFile))
      if(generateNegative) {
        referenceLinks = referenceLinks.generateNegative
      }
      project.updateTask(taskName, task.data.copy(referenceLinks = referenceLinks))
    }
    Ok
  }

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
  def deleteReferenceLinks(projectName: String, taskName: String,
                           positive: Boolean, negative: Boolean, unlabeled: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val referenceLinks = task.data.referenceLinks

    val newReferenceLinks =
      ReferenceLinks(
        positive = if(positive) Set.empty else referenceLinks.positive,
        negative = if(negative) Set.empty else referenceLinks.negative,
        unlabeled = if(unlabeled) Set.empty else referenceLinks.unlabeled
      )
    task.update(task.data.copy(referenceLinks = newReferenceLinks))

    Ok
  }


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
  def putReferenceLink(projectName: String, taskName: String, linkType: String, source: String, target: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    log.info(s"Adding $linkType reference link: $source - $target")
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
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
  def deleteReferenceLink(projectName: String, taskName: String, source: String, target: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val link = new Link(source, target)
    
    val updatedTask = task.data.copy(referenceLinks = task.data.referenceLinks.without(link))
    project.updateTask(taskName, updatedTask)
    
    Ok
  }

  def reloadLinkingCache(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val referenceEntitiesCache = task.activity[ReferenceEntitiesCache].control
    referenceEntitiesCache.reset()
    referenceEntitiesCache.start()
    Ok
  }

  def writeReferenceLinks(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val params = request.body.asFormUrlEncoded.get

    for(posOutputName <- params.get("positiveOutput")) {
      val posOutput = project.task[GenericDatasetSpec](posOutputName.head).data.linkSink
      posOutput.writeLinks(task.data.referenceLinks.positive, params("positiveProperty").head)
    }

    for(negOutputName <- params.get("negativeOutput")) {
      val negOutput = project.task[GenericDatasetSpec](negOutputName.head).data.linkSink
      negOutput.writeLinks(task.data.referenceLinks.negative, params("negativeProperty").head)
    }

    Ok
  }

  def learningActivity(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    task.activity[LearningActivity].control.start()
    Ok
  }

  def activeLearningActivity(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val learningActivity = task.activity[ActiveLearning]

    if(task.data.referenceLinks.unlabeled.isEmpty) {
      val updatedReferenceLinks = task.data.referenceLinks.copy(unlabeled = learningActivity.value().pool.links.toSet)
      task.update(task.data.copy(referenceLinks = updatedReferenceLinks))
    }

    learningActivity.control.start()
    Ok
  }

  // Get the project and linking task
  private def projectAndTask(projectName: String, taskName: String)
                            (implicit userContext: UserContext): (Project, ProjectTask[LinkSpec]) = {
    getProjectAndTask[LinkSpec](projectName, taskName)
  }

  def referenceLinksEvaluated(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val rule = task.data.rule
    implicit val writeContext = WriteContext[JsValue]()

    // Make sure that the reference entities cache is up-to-date
    val referenceEntitiesCache = task.activity[ReferenceEntitiesCache].control
    if(referenceEntitiesCache.status().isRunning) {
      referenceEntitiesCache.waitUntilFinished()
    }
    referenceEntitiesCache.startBlocking()
    val referenceEntities = referenceEntitiesCache.value()

    def serializeLinks(entities: Traversable[DPair[Entity]]): JsValue = {
      JsArray(
        for(entities <- entities.toSeq) yield {
          val link = new Link(entities.source.uri, entities.target.uri, Some(rule(entities)), Some(entities))
          new LinkJsonFormat(Some(rule)).write(link)
        }
      )
    }

    val result =
      Json.obj(
        "positive" -> serializeLinks(referenceEntities.positiveEntities),
        "negative" -> serializeLinks(referenceEntities.negativeEntities)
      )

    Ok(result)
  }

  /** Executes a linking task on the data sources given in the request. */
  def postLinkDatasource(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    request.body match {
      case AnyContentAsXml(xmlRoot) =>
        try{
          val (_, task) = projectAndTask(projectName, taskName)
          implicit val (resourceManager, _) = createInMemoryResourceManagerForResources(xmlRoot, projectName, withProjectResources = true)
          val linkSource = createDataSource(xmlRoot, Some("sourceDataset"))
          val linkTarget = createDataSource(xmlRoot, Some("targetDataset"))
          val (model, linkSink) = createLinkSink(xmlRoot)
          val link = new GenerateLinksActivity(taskName, task.taskLabel(), DPair(linkSource, linkTarget), task.data, Seq(linkSink))
          Activity(link).startBlocking()
          val acceptedContentType = request.acceptedTypes.headOption.map(_.mediaType).getOrElse("application/n-triples")
          result(model, acceptedContentType, "Successfully generated links")
        } catch {
          case _: NoSuchElementException =>
            throw new NotFoundException("Not found")
        }
      case _ =>
        throw UnsupportedMediaTypeException.supportedFormats("application/xml")
    }
  }

  /**
    * Evaluates a linkage rule specified in the request on the linking task.
    * This endpoint can be used to test temporary, alternative linkage rules without having to persist them first.
    *
    * @param linkLimit   Max. number of links that should be returned.
    * @param timeoutInMs Max. runtime in milliseconds the matching task should run
    * @return
    */
  def evaluateLinkageRule(projectName: String,
                          linkingTaskName: String,
                          linkLimit: Int,
                          timeoutInMs: Int,
                          includeReferenceLinks: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[LinkSpec](projectName, linkingTaskName)
    val sources = task.dataSources
    implicit val readContext: ReadContext = ReadContext(prefixes = project.config.prefixes, resources = project.resources)
    SerializationUtils.deserializeCompileTime[LinkageRule](defaultMimeType = SerializationUtils.APPLICATION_JSON) { linkageRule =>
      val updatedLinkSpec = task.data.copy(rule = linkageRule)
      val runtimeConfig = RuntimeLinkingConfig(executionTimeout = Some(timeoutInMs), linkLimit = Some(linkLimit),
        generateLinksWithEntities = true, includeReferenceLinks = includeReferenceLinks)
      val linksActivity = new GenerateLinksActivity(linkingTaskName, task.taskLabel(), sources, updatedLinkSpec, Seq(), runtimeConfig)
      val control = Activity(linksActivity)
      control.startBlocking()
      control.value.get match {
        case Some(linking) =>
          val linkJsonFormat = new LinkJsonFormat(Some(linking.rule))
          implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue]()
          Ok(JsArray(
            for(link <- linking.links) yield {
              linkJsonFormat.write(link)
            }
          ))
        case None =>
          InternalServerError
      }
    }
  }
}