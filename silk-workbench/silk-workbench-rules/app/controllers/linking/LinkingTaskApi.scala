package controllers.linking

import controllers.core.UserContextActions
import controllers.linking.doc.LinkingTaskApiDoc
import controllers.util.ProjectUtils._
import controllers.util.SerializationUtils
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.config.{MetaData, PlainTask, Prefixes}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.{Entity, FullLink, MinimalLink, Restriction}
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
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._
import org.silkframework.workspace.activity.linking.ReferenceEntitiesCache
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{Action, AnyContent, AnyContentAsXml, InjectedController}

import java.util.logging.{Level, Logger}
import javax.inject.Inject

@Tag(name = "Linking")
class LinkingTaskApi @Inject() () extends InjectedController with UserContextActions {

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
    implicit val prefixes: Prefixes = proj.config.prefixes

    val sourceDataset = DatasetSelection(values("source"), Uri.parse(values.getOrElse("sourceType", ""), prefixes), Restriction.custom(values.getOrElse("sourceRestriction", "")))
    val targetDataset = DatasetSelection(values("target"), Uri.parse(values.getOrElse("targetType", ""), prefixes), Restriction.custom(values.getOrElse("targetRestriction", "")))
    val output = values.get("output").filter(_.nonEmpty).map(str => str.split(",").map(Identifier(_))).toSeq.flatten.headOption

    proj.tasks[LinkSpec].find(_.id.toString == task) match {
      //Update existing task
      case Some(oldTask) if !createOnly => {
        val updatedLinkSpec = oldTask.data.copy(source = sourceDataset, target = targetDataset, output = output)
        proj.updateTask(task, updatedLinkSpec)
      }
      //Create new task
      case _ => {
        val linkSpec =
          LinkSpec(
            source = sourceDataset,
            target = targetDataset,
            rule = LinkageRule(None),
            output = output
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

  @Operation(
    summary = "Retrieve reference links",
    description = "Retrieve all reference links of a specified linking task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        content = Array(
          new Content(
            mediaType = "application/xml",
            examples = Array(new ExampleObject(LinkingTaskApiDoc.referenceLinksExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  def getReferenceLinks(@Parameter(
                          name = "project",
                          description = "The project identifier",
                          required = true,
                          in = ParameterIn.PATH,
                          schema = new Schema(implementation = classOf[String])
                        )
                        projectName: String,
                        @Parameter(
                          name = "task",
                          description = "The task identifier",
                          required = true,
                          in = ParameterIn.PATH,
                          schema = new Schema(implementation = classOf[String])
                        )
                        taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val referenceLinksXml = task.data.referenceLinks.toXML

    Ok(referenceLinksXml).withHeaders("Content-Disposition" -> s"attachment; filename=referenceLinks.xml")
  }

  @Operation(
    summary = "Update reference links",
    description = "Update all reference links of a specified linking task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the reference links have been updated successfully"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(
      new Content(
        mediaType = "application/xml",
        examples = Array(new ExampleObject(LinkingTaskApiDoc.referenceLinksExample))
      )
    )
  )
  def putReferenceLinks(@Parameter(
                          name = "project",
                          description = "The project identifier",
                          required = true,
                          in = ParameterIn.PATH,
                          schema = new Schema(implementation = classOf[String])
                        )
                        projectName: String,
                        @Parameter(
                          name = "task",
                          description = "The task identifier",
                          required = true,
                          in = ParameterIn.PATH,
                          schema = new Schema(implementation = classOf[String])
                        )
                        taskName: String,
                        @Parameter(
                          name = "generateNegative",
                          description = "If true, negative reference links will be generated by interchanging the source and targets of the provided positive links. This will only produce correct results if the provided positive reference links are complete and model 1:1 relations.",
                          required = true,
                          in = ParameterIn.QUERY,
                          schema = new Schema(implementation = classOf[Boolean])
                        )
                        generateNegative: Boolean): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
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

  @Operation(
    summary = "Delete reference links",
    description = "Removes reference links from a specified linking task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the reference links have been deleted successfully"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  def deleteReferenceLinks(@Parameter(
                             name = "project",
                             description = "The project identifier",
                             required = true,
                             in = ParameterIn.PATH,
                             schema = new Schema(implementation = classOf[String])
                           )
                           projectName: String,
                           @Parameter(
                             name = "task",
                             description = "The task identifier",
                             required = true,
                             in = ParameterIn.PATH,
                             schema = new Schema(implementation = classOf[String])
                           )
                           taskName: String,
                           @Parameter(
                             name = "positive",
                             description = "If true, positive reference links will be deleted.",
                             required = true,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[Boolean])
                           )
                           positive: Boolean,
                           @Parameter(
                             name = "negative",
                             description = "If true, negative reference links will be deleted.",
                             required = true,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[Boolean])
                           )
                           negative: Boolean,
                           @Parameter(
                             name = "unlabeled",
                             description = "If true, unlabeled reference links will be deleted.",
                             required = true,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[Boolean])
                           )
                           unlabeled: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
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

  @Operation(
    summary = "Add reference link",
    description = "Add a reference link to a specific linking task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the reference link has been added successfully"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  def putReferenceLink(@Parameter(
                         name = "project",
                         description = "The project identifier",
                         required = true,
                         in = ParameterIn.PATH,
                         schema = new Schema(implementation = classOf[String])
                       )
                       projectName: String,
                       @Parameter(
                         name = "task",
                         description = "The task identifier",
                         required = true,
                         in = ParameterIn.PATH,
                         schema = new Schema(implementation = classOf[String])
                       )
                       taskName: String,
                       @Parameter(
                         name = "linkType",
                         description = "The link type, either \"negative\" or \"positive\".",
                         required = true,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String], allowableValues = Array("positive", "negative"))
                       )
                       linkType: String,
                       @Parameter(
                         name = "source",
                         description = "The source entity URI.",
                         required = true,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String])
                       )
                       source: String,
                       @Parameter(
                         name = "target",
                         description = "The target entity URI.",
                         required = true,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String])
                       )
                       target: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    log.info(s"Adding $linkType reference link: $source - $target")
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val link = new MinimalLink(source, target)

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

  @Operation(
    summary = "Remove reference link",
    description = "Remove a reference link from specific linking task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the reference link has been removed successfully"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  def deleteReferenceLink(@Parameter(
                            name = "project",
                            description = "The project identifier",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          projectName: String,
                          @Parameter(
                            name = "task",
                            description = "The task identifier",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          taskName: String,
                          @Parameter(
                            name = "source",
                            description = "The source entity URI.",
                            required = true,
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[String])
                          )
                          source: String,
                          @Parameter(
                            name = "target",
                            description = "The target entity URI.",
                            required = true,
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[String])
                          )
                          target: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val link = new MinimalLink(source, target)
    
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

    learningActivity.control.start()
    Ok
  }

  // Get the project and linking task
  private def projectAndTask(projectName: String, taskName: String)
                            (implicit userContext: UserContext): (Project, ProjectTask[LinkSpec]) = {
    getProjectAndTask[LinkSpec](projectName, taskName)
  }

  @Operation(
    summary = "Evaluate on reference links",
    description = "Evaluates the current linking rule on all reference links.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(LinkingTaskApiDoc.referenceLinksEvaluatedExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  def referenceLinksEvaluated(@Parameter(
                                name = "project",
                                description = "The project identifier",
                                required = true,
                                in = ParameterIn.PATH,
                                schema = new Schema(implementation = classOf[String])
                              )
                              projectName: String,
                              @Parameter(
                                name = "task",
                                description = "The task identifier",
                                required = true,
                                in = ParameterIn.PATH,
                                schema = new Schema(implementation = classOf[String])
                              )
                              taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
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
          val link = new FullLink(entities.source.uri, entities.target.uri, rule(entities), entities)
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

  @Operation(
    summary = "Linking task execution with payload",
    description = "Execute a specific linking rule against input data from the POST body.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        content = Array(
          new Content(
            mediaType = "application/n-triples",
            examples = Array(new ExampleObject(LinkingTaskApiDoc.postLinkDatasourceResponseExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(
      new Content(
        mediaType = "application/xml",
        examples = Array(new ExampleObject(LinkingTaskApiDoc.postLinkDatasourceRequestExample))
      )
    )
  )
  def postLinkDatasource(@Parameter(
                           name = "project",
                           description = "The project identifier",
                           required = true,
                           in = ParameterIn.PATH,
                           schema = new Schema(implementation = classOf[String])
                         )
                         projectName: String,
                         @Parameter(
                           name = "task",
                           description = "The task identifier",
                           required = true,
                           in = ParameterIn.PATH,
                           schema = new Schema(implementation = classOf[String])
                         )
                         taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    request.body match {
      case AnyContentAsXml(xmlRoot) =>
        try{
          val (_, task) = projectAndTask(projectName, taskName)
          implicit val (resourceManager, _) = createInMemoryResourceManagerForResources(xmlRoot, projectName, withProjectResources = true)
          val linkSource = createDataSource(xmlRoot, Some("sourceDataset"))
          val linkTarget = createDataSource(xmlRoot, Some("targetDataset"))
          val (model, linkSink) = createLinkSink(xmlRoot)
          val link = new GenerateLinksActivity(task, DPair(linkSource, linkTarget), Some(linkSink))
          Activity(link).startBlocking()
          val acceptedContentType = request.acceptedTypes.headOption.map(_.mediaType).getOrElse("application/n-triples")
          result(model, acceptedContentType, "Successfully generated links")
        } catch {
          case ex: NoSuchElementException =>
            throw new NotFoundException("Not found", Some(ex))
        }
      case _ =>
        throw UnsupportedMediaTypeException.supportedFormats("application/xml")
    }
  }

  @Operation(
    summary = "Evaluate provided linking rule",
    description = "Evaluate a linking task based on a linkage rule that is provided with the request. This endpoint can be used to test temporary, alternative linkage rules without having to persist them first.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(LinkingTaskApiDoc.evaluateLinkageRuleResponseExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        examples = Array(new ExampleObject(LinkingTaskApiDoc.evaluateLinkageRuleRequestJsonExample))
      ),
      new Content(
        mediaType = "application/xml",
        examples = Array(new ExampleObject(LinkingTaskApiDoc.evaluateLinkageRuleRequestXmlExample))
      )
    )
  )
  def evaluateLinkageRule(@Parameter(
                            name = "project",
                            description = "The project identifier",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          projectName: String,
                          @Parameter(
                            name = "linkingTaskName",
                            description = "The task identifier",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          linkingTaskName: String,
                          @Parameter(
                            name = "linkLimit",
                            description = "The max. number of unique links that should be returned from the evaluation.",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[Int], defaultValue = "1000")
                          )
                          linkLimit: Int,
                          @Parameter(
                            name = "timeoutInMs",
                            description = "The max. time in milliseconds the matching stage of the linking execution is allowed to run. This timeout does not affect the loading stage.",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[Int], defaultValue = "30000")
                          )
                          timeoutInMs: Int,
                          @Parameter(
                            name = "includeReferenceLinks",
                            description = "When true, this will return an evaluation of the reference links in addition to freshly matched links.",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                          )
                          includeReferenceLinks: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[LinkSpec](projectName, linkingTaskName)
    val sources = task.dataSources
    implicit val readContext: ReadContext = ReadContext(prefixes = project.config.prefixes, resources = project.resources)
    SerializationUtils.deserializeCompileTime[LinkageRule](defaultMimeType = SerializationUtils.APPLICATION_JSON) { linkageRule =>
      val updatedLinkSpec = task.data.copy(rule = linkageRule)
      val updatedTask = PlainTask(linkingTaskName, updatedLinkSpec, task.metaData)
      val runtimeConfig = RuntimeLinkingConfig(executionTimeout = Some(timeoutInMs), linkLimit = Some(linkLimit),
        generateLinksWithEntities = true, includeReferenceLinks = includeReferenceLinks)
      val linksActivity = new GenerateLinksActivity(updatedTask, sources, None, runtimeConfig)
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
          ErrorResult(INTERNAL_SERVER_ERROR, "No value generated", "The linking tasks did not generate any value.")
      }
    }
  }
}