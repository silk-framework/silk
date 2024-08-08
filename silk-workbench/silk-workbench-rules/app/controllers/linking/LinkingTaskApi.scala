package controllers.linking

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.linking.doc.LinkingTaskApiDoc
import controllers.linking.evaluation.{AddPathToReferenceEntitiesCacheRequest, EvaluateCurrentLinkageRuleRequest, LinkRuleEvaluationStats, LinkageRuleEvaluationResult}
import controllers.linking.linkingTask.LinkingTaskApiUtils
import controllers.util.ProjectUtils._
import controllers.util.SerializationUtils
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.{FixedNumberOfInputs, FixedSchemaPort, MetaData, PlainTask, Prefixes}
import org.silkframework.dataset.Dataset
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.plugins.path.{PathMetaDataPlugin, StandardMetaDataPlugin}
import org.silkframework.rule.evaluation._
import org.silkframework.rule.execution.{Linking, GenerateLinks => GenerateLinksActivity}
import org.silkframework.rule.{DatasetSelection, LinkSpec, LinkageRule, RuntimeLinkingConfig}
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlSerialization}
import org.silkframework.runtime.validation._
import org.silkframework.serialization.json.JsonSerialization
import org.silkframework.serialization.json.JsonSerializers.LinkageRuleJsonFormat
import org.silkframework.serialization.json.LinkingSerializers.LinkJsonFormat
import org.silkframework.util.Identifier._
import org.silkframework.util.{DPair, Identifier, StringUtils, Uri}
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._
import org.silkframework.workspace.activity.linking.{EvaluateLinkingActivity, LinkingPathsCache, ReferenceEntitiesCache}
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json._
import play.api.mvc.{Action, _}

import java.util.logging.{Level, Logger}
import javax.inject.Inject
import scala.collection.mutable

@Tag(name = "Linking", description = "Linking specific operations, such as evaluating rules and managing reference links.")
class LinkingTaskApi @Inject() (accessMonitor: WorkbenchAccessMonitor) extends InjectedController with UserContextActions with ControllerUtilsTrait {

  private val log = Logger.getLogger(getClass.getName)

  /** Fetches a linking task. */
  def getLinkingTask(projectName: String,
                     taskName: String,
                     withLabels: Boolean,
                     langPref: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val (project, task) = getProjectAndTask[LinkSpec](projectName, taskName)
    accessMonitor.saveProjectTaskAccess(project.config.id, task.id)
    if(withLabels) {
      val linkSpec = task.data
      val DPair(sourcePaths, targetPaths) = linkSpec.entityDescriptions.map(es => es.typedPaths)
      val sourcePathLabels = pathLabels(task.project, linkSpec.source.inputId, sourcePaths, langPref)
      val targetPathLabels = pathLabels(task.project, linkSpec.target.inputId, targetPaths, langPref)
      Ok(LinkingTaskApiUtils.getLinkSpecWithRuleNodeParameterValueLabels(task, sourcePathLabels, targetPathLabels))
    } else {
      SerializationUtils.serializeCompileTime[LinkSpec](task.data, Some(project))
    }
  }

  private def pathLabels(project: Project,
                         dataSourceTaskId: String,
                         typedPaths: Seq[TypedPath],
                         langPref: String)
                        (implicit userContext: UserContext): Map[String, String] = {
    implicit val prefixes: Prefixes = project.config.prefixes
    val result = new mutable.HashMap[String, String]()
    def dataset: Option[ProjectTask[GenericDatasetSpec]] = project.taskOption[GenericDatasetSpec](dataSourceTaskId)
    dataset.flatMap(d => datasetPathMetaDataPlugin(d)).foreach { pathMetaDataPlugin =>
      val pathsMetaData = pathMetaDataPlugin.fetchMetaData(dataset.get.data.plugin, typedPaths, langPref)
      pathsMetaData
        .foreach(pmd =>
          pmd.label.foreach { label =>
            result.put(pmd.value, label)
            result.put(UntypedPath.parse(pmd.value).serialize()(Prefixes.empty), label)
          }
        )

    }
    result.toMap
  }

  def pushLinkingTask(project: String, task: String, createOnly: Boolean): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val values = request.body.asFormUrlEncoded.getOrElse(request.queryString).view.mapValues(_.head).toMap

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

        proj.addTask(task, linkSpec, MetaData.empty)
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
    implicit val readContext: ReadContext = ReadContext.fromProject(project)

    try {
      val updatedRule = linkageRule(request)
      val validationIssues = updatedRule.validate()
      if(validationIssues.isEmpty) {
        // Commit rule
        project.updateTask(taskName, task.data.copy(rule = updatedRule))
        ErrorResult.validation(OK, "Linkage rule committed successfully", issues = Seq.empty)
      } else {
        // Return issues
        ErrorResult.validation(BAD_REQUEST, "Linkage rule with warnings", issues = validationIssues)
      }
    } catch {
      case ex: BadUserInputException =>
        throw ex
      case ex: ValidationException =>
        log.log(Level.INFO, "Invalid linkage rule")
        ErrorResult.validation(BAD_REQUEST, "Invalid linkage rule", issues = ex.errors)
      case ex: Exception =>
        log.log(Level.INFO, "Failed to commit linkage rule", ex)
        ErrorResult.validation(INTERNAL_SERVER_ERROR, "Failed to commit linkage rule", issues = ValidationError("Error in back end: " + ex.getMessage) :: Nil)
    }
  }

  private def linkageRule(request: Request[AnyContent])
                         (implicit readContext: ReadContext): LinkageRule = {
    request.body.asXml match {
      case Some(xml) =>
        XmlSerialization.fromXml[LinkageRule](xml.head)
      case None =>
        request.body.asJson match {
          case Some(json) =>
            JsonSerialization.fromJson[LinkageRule](json)
          case None =>
            throw BadUserInputException("Expecting text/xml or application/json request body")
        }
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
          //Load link specification
          val newLinkSpec = XmlSerialization.fromXml[LinkSpec](xml.head)
          val issues = newLinkSpec.rule.validate()
          if(issues.isEmpty) {
            //Update linking task
            project.updateTask(taskName, newLinkSpec.copy(referenceLinks = task.data.referenceLinks))
            ErrorResult.validation(OK, "Linkage rule committed successfully", issues = Seq.empty)
          } else {
            ErrorResult.validation(OK, "Linkage rule with warnings", issues = issues)
          }
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
        description = "Success",
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
        schema = new Schema(implementation = classOf[String]),
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
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val link = new MinimalLink(checkUri(source), checkUri(target))
    log.info(s"Adding $linkType reference link: ${link.source} - ${link.target}")

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

  private def checkUri(uri: String): String = {
    val trimmed = uri.trim
    if(!Uri(trimmed).isValidUri) {
      throw BadUserInputException(s"Uri '$trimmed' is not a valid, absolute URI!")
    }
    trimmed
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

  // All path meta data plugins
  private lazy val pathMetaDataPlugins: Map[Class[_], PathMetaDataPlugin[_]] = LinkingTaskApiUtils.pathMetaDataPlugins

  private def datasetPathMetaDataPlugin(datasetTask: ProjectTask[GenericDatasetSpec]): Option[PathMetaDataPlugin[Dataset]] = {
    pathMetaDataPlugins.get(datasetTask.data.plugin.getClass).map(_.asInstanceOf[PathMetaDataPlugin[Dataset]])
  }
  private val standardMetaDataPlugin = StandardMetaDataPlugin()

  /** Fetches the linking path cache value.  TODO: Add swagger annotation when finalized
    *
    * @param target If the target paths should be fetches, else the source paths are fetched.
    */
  def getLinkingPathCacheValue(projectId: String,
                               linkingTaskId: String,
                               target: Boolean,
                               withMetaData: Boolean,
                               langPref: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val (project, linkingTask) = getProjectAndTask[LinkSpec](projectId, linkingTaskId)
    implicit val prefixes: Prefixes = project.config.prefixes
    linkingPathCacheValues(linkingTask) match {
      case Some(value) =>
        val (sourceTaskId, sourceEntitySchema) = if(target) {
          (linkingTask.data.target.inputId, value.target)
        } else {
          (linkingTask.data.source.inputId, value.source)
        }
        if(withMetaData) {
          // For now we only support dataset plugins
          def dataset: Option[ProjectTask[GenericDatasetSpec]] = project.taskOption[GenericDatasetSpec](sourceTaskId)
          val pathsWithMetaData = dataset.flatMap(d => datasetPathMetaDataPlugin(d)) match {
            case Some(pathMetaDataPlugin) =>
              pathMetaDataPlugin.fetchMetaData(dataset.get.data.plugin, sourceEntitySchema.typedPaths, langPref)
            case None =>
              standardMetaDataPlugin.fetchMetaData(standardMetaDataPlugin, sourceEntitySchema.typedPaths, langPref)
          }
          Ok(Json.toJson(pathsWithMetaData.toSeq))
        } else {
          Ok(Json.toJson(sourceEntitySchema.typedPaths.map(_.serialize())))
        }
      case None =>
        throw NotFoundException("No cached value available.")
    }
  }

  private def linkingPathCacheValues(linkingTask: ProjectTask[LinkSpec]): Option[DPair[EntitySchema]] = {
    val linkingPathsCache = linkingTask.activity[LinkingPathsCache]
    linkingPathsCache.value.get
  }

  def writeReferenceLinks(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val params = request.body.asFormUrlEncoded.get
    implicit val prefixes: Prefixes = project.config.prefixes

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

  @Operation(
    summary = "Add path to the reference entities cache",
    description = "Adds a path to the reference entities cache.",
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "Success"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  def addPathToReferenceEntitiesCache(@Parameter(
                                        name = "project",
                                        description = "The project identifier",
                                        required = true,
                                        in = ParameterIn.PATH,
                                        schema = new Schema(implementation = classOf[String])
                                      )
                                      projectId: String,
                                      @Parameter(
                                        name = "task",
                                        description = "The task identifier",
                                        required = true,
                                        in = ParameterIn.PATH,
                                        schema = new Schema(implementation = classOf[String])
                                      )
                                      linkingTaskId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    val (project, linkingTask) = getProjectAndTask[LinkSpec](projectId, linkingTaskId)
    validateJson[AddPathToReferenceEntitiesCacheRequest] { request =>
      implicit val prefixes: Prefixes = project.config.prefixes
      val typedPath = UntypedPath.parse(request.path).asStringTypedPath
      val entitiesCacheControl = linkingTask.activity[ReferenceEntitiesCache].control
      entitiesCacheControl.underlying match {
        case entitiesCache: ReferenceEntitiesCache =>
          if(request.toTarget) {
           entitiesCache.addTargetPathToCache(typedPath)
          } else {
            entitiesCache.addSourcePathToCache(typedPath)
          }
          if(request.reloadCache) {
            entitiesCacheControl.startBlocking()
          }
          NoContent
        case _ =>
          throw NotFoundException("No entities cache found.")
      }
    }
  }

  // Get the project and linking task
  private def projectAndTask(projectName: String, taskName: String)
                            (implicit userContext: UserContext): (Project, ProjectTask[LinkSpec]) = {
    getProjectAndTask[LinkSpec](projectName, taskName)
  }

  private def updateAndGetReferenceEntityCacheValue(task: ProjectTask[LinkSpec],
                                                    refreshCache: Boolean)
                                                   (implicit userContext: UserContext): ReferenceEntities = {
    // Make sure that the reference entities cache is up-to-date
    val referenceEntitiesCache = task.activity[ReferenceEntitiesCache].control
    if(referenceEntitiesCache.status().isRunning) {
      referenceEntitiesCache.waitUntilFinished()
    }
    if(refreshCache) {
      referenceEntitiesCache.startBlocking()
    }
    referenceEntitiesCache.value()
  }

  private def serializeLinks(entities: Iterable[DPair[Entity]],
                             linkageRule: LinkageRule,
                             withEntitiesAndSchema: Boolean = false)
                            (implicit writeContext: WriteContext[JsValue]): JsValue = {
    JsArray(
      for(entities <- entities.toSeq) yield {
        val link = new FullLink(entities.source.uri, entities.target.uri, linkageRule(entities), entities)
        new LinkJsonFormat(Some(linkageRule), writeEntities = withEntitiesAndSchema, writeEntitySchema = withEntitiesAndSchema).write(link)
      }
    )
  }

  @Operation(
    summary = "Evaluate on reference links",
    description = "Evaluates the current linking rule on all reference links.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
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
                              taskName: String,
                              @Parameter(
                                name = "withEntitiesAndSchema",
                                description = "When set to true each link contains the entities and the schema",
                                required = false,
                                in = ParameterIn.QUERY,
                                schema = new Schema(implementation = classOf[Boolean], defaultValue = "false"),
                              )
                              withEntitiesAndSchema: Boolean): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val rule = task.data.rule

    val referenceEntityCacheValue = updateAndGetReferenceEntityCacheValue(task, refreshCache = true)
    val evaluationResult: LinkageRuleEvaluationResult = LinkingTaskApiUtils.referenceLinkEvaluationScore(task.data.rule, referenceEntityCacheValue)

    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
    val result =
      Json.obj(
        "positive" -> serializeLinks(referenceEntityCacheValue.positiveEntities, rule, withEntitiesAndSchema),
        "negative" -> serializeLinks(referenceEntityCacheValue.negativeEntities, rule, withEntitiesAndSchema),
        "evaluationScore" -> Json.toJson(evaluationResult)
      )

    Ok(result)
  }

  @Operation(
    summary = "Evaluate linkage rule against reference links",
    description = "Evaluates a linkage rule send with the requests on all reference links of the linking task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
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
  def referenceLinksEvaluateLinkageRule(@Parameter(name = "project", in = ParameterIn.PATH, description = "The project identifier", required = true,
                                          schema = new Schema(implementation = classOf[String])
                                        )
                                        projectName: String,
                                        @Parameter(name = "linkingTaskId", in = ParameterIn.PATH, description = "The task identifier", required = true,
                                          schema = new Schema(implementation = classOf[String])
                                        )
                                        linkingTaskId: String,
                                        @Parameter(name = "linkLimit", in = ParameterIn.QUERY, description = "The max. number of unique links that should be returned for each link categorty, i.e. psitive, negative.", required = false,
                                          schema = new Schema(implementation = classOf[Int], defaultValue = "1000")
                                        )
                                        linkLimit: Int): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[LinkSpec](projectName, linkingTaskId)
    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    implicit val prefixes: Prefixes = project.config.prefixes

    SerializationUtils.deserializeCompileTime[LinkageRule](defaultMimeType = SerializationUtils.APPLICATION_JSON) { linkageRule =>
      val referenceEntityCacheValue = updateAndGetReferenceEntityCacheValue(task, refreshCache = false)
      implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
      def serialize(links: Iterable[DPair[Entity]]): JsValue = {
        serializeLinks(links.take(linkLimit), linkageRule)
      }
      val evaluationResult: LinkageRuleEvaluationResult = LinkingTaskApiUtils.referenceLinkEvaluationScore(linkageRule, referenceEntityCacheValue)

      try {
        val result =
          Json.obj(
            "positive" -> serialize(referenceEntityCacheValue.positiveEntities),
            "negative" -> serialize(referenceEntityCacheValue.negativeEntities),
            "evaluationScore" -> Json.toJson(evaluationResult)
          )

        Ok(result)
      } catch {
        case NoSuchPathException(_, path) => {
          // Signal to the user that there is a problem with the current state of the linking rule that needs to be solved by the user.
          throw ConflictRequestException(path.serialize())
        }
      }
    }
  }

  @Operation(
    summary = "Linking task execution with payload",
    description = "Execute a specific linking rule against input data from the POST body.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/n-triples",
            schema = new Schema(implementation = classOf[String]),
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
        schema = new Schema(implementation = classOf[String]),
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
          val (project, task) = projectAndTask(projectName, taskName)
          implicit val prefixes: Prefixes = project.config.prefixes
          implicit val (resourceManager, _) = createInMemoryResourceManagerForResources(xmlRoot, projectName, withProjectResources = true, None)
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
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(`type` = "object"),
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
        schema = new Schema(`type` = "object"),
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
    implicit val prefixes: Prefixes = project.config.prefixes

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
          implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
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

  @Operation(
    summary = "Evaluate current linking rule",
    description = "Evaluate a linking task based on its current linkage rule.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(`type` = "object"),
            examples = Array(new ExampleObject(LinkingTaskApiDoc.evaluateCurrentLinkageRuleExample))
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
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(LinkingTaskApiDoc.evaluateCurrentLinkageRuleRequest))
      )
    )
  )
  def evaluateCurrentLinkageRule(@Parameter(
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
                                 ): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    implicit val (project, linkTask) = getProjectAndTask[LinkSpec](projectName, linkingTaskName)

    validateJson[EvaluateCurrentLinkageRuleRequest] { request =>
      processLinkingTaskRequest(linkTask, request)
    }
  }

  private def filterLinks(links: Seq[EvaluatedLinkWithDecision],
                          referenceEntities: ReferenceEntitiesCache#ValueType,
                          filters: Seq[EvaluateCurrentLinkageRuleRequest.EvaluationLinkFilterEnum.Value]): Seq[EvaluatedLinkWithDecision] = {
    val validLinkDecisions = filters.map {
      case EvaluateCurrentLinkageRuleRequest.EvaluationLinkFilterEnum.negativeLinks =>
        LinkDecision.NEGATIVE
      case EvaluateCurrentLinkageRuleRequest.EvaluationLinkFilterEnum.positiveLinks =>
        LinkDecision.POSITIVE
      case EvaluateCurrentLinkageRuleRequest.EvaluationLinkFilterEnum.undecidedLinks =>
        LinkDecision.UNLABELED
    }.toSet
    def filterLink(link: Link): Boolean = {
      validLinkDecisions.contains(referenceEntities.linkDecision(link))
    }
    links.filter(filterLink)
  }

  // Sorts the links by all given sort criteria, which are applied in a hierarchical manner, i.e. first criterion sorts the first level.
  private def sortLinks(links: Seq[EvaluatedLinkWithDecision],
                        sortBy: List[EvaluateCurrentLinkageRuleRequest.EvaluationLinkSortEnum.Value]): Seq[EvaluatedLinkWithDecision] = {
    def compareTo(linkA: Link,
                  linkB: Link,
                  by: EvaluateCurrentLinkageRuleRequest.EvaluationLinkSortEnum.Value): Int = {
      val confidenceA = linkA.confidence.getOrElse(0.0)
      val confidenceB = linkB.confidence.getOrElse(0.0)

      by match {
        case EvaluateCurrentLinkageRuleRequest.EvaluationLinkSortEnum.scoreAsc =>
          confidenceA.compareTo(confidenceB)
        case EvaluateCurrentLinkageRuleRequest.EvaluationLinkSortEnum.scoreDesc =>
          confidenceA.compareTo(confidenceB) * -1
        case EvaluateCurrentLinkageRuleRequest.EvaluationLinkSortEnum.sourceEntityAsc =>
          linkA.source.compareTo(linkB.source)
        case EvaluateCurrentLinkageRuleRequest.EvaluationLinkSortEnum.sourceEntityDesc =>
          linkA.source.compareTo(linkB.source) * -1
        case EvaluateCurrentLinkageRuleRequest.EvaluationLinkSortEnum.targetEntityAsc =>
          linkA.target.compareTo(linkB.target)
        case EvaluateCurrentLinkageRuleRequest.EvaluationLinkSortEnum.targetEntityDesc =>
          linkA.target.compareTo(linkB.target) * -1
      }
    }
    def sort(linkA: Link, linkB: Link): Boolean = {
      var comparisonResult = 0.0
      var sortByStack = sortBy
      while(comparisonResult == 0.0 && sortByStack.nonEmpty) {
        val nextSortBy = sortByStack.head
        sortByStack = sortByStack.tail
        comparisonResult = compareTo(linkA, linkB, nextSortBy)
      }
      comparisonResult < 0
    }
    links.sortWith(sort)
  }

  private def processLinkingTaskRequest(linkTask: ProjectTask[LinkSpec],
                                        request: EvaluateCurrentLinkageRuleRequest)
                                       (implicit userContext: UserContext): Result = {
    val offset = request.offset.getOrElse(0)
    val limit = request.limit.getOrElse(20)
    val filters = request.filters.getOrElse(Seq.empty)
    val sortBy = request.sortBy.getOrElse(List.empty)
    val query = request.query.getOrElse("")
    val includeReferenceLinks = request.includeReferenceLinks.getOrElse(false)
    val includeEvaluationLinks = request.includeEvaluationLinks.getOrElse(true)
    val project = linkTask.project
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue](prefixes = project.config.prefixes, resources = project.resources)
    val evaluationActivity = linkTask.activity[EvaluateLinkingActivity]
    val referenceEntityCache = linkTask.activity[ReferenceEntitiesCache].value()
    var links: Seq[EvaluatedLinkWithDecision] = Seq.empty
    var evaluationStats: Option[LinkRuleEvaluationStats] = None
    if(includeEvaluationLinks) {
      links = retrieveEvaluationLinks(linkTask)
      evaluationStats = Some(linkEvaluationStats(evaluationActivity.value()))
    }
    if (includeReferenceLinks) {
      links = (retrieveReferenceLinksSafe(linkTask) ++ links).distinct
    }
    val linkingRule = linkTask.data.rule
    val overallLinkCount = links.size
    val searchTerms = StringUtils.extractSearchTerms(query)
    if (searchTerms.nonEmpty) {
      val (usedPathsSource, usedPathTarget) = schemaPathFunctions(linkTask.data)
      links = links.filter(link => {
        val searchText = searchStringFromLink(link, usedPathsSource, usedPathTarget)
        StringUtils.matchesSearchTerm(searchTerms, searchText)
      })
    }
    if (filters.nonEmpty) {
      links = filterLinks(links, referenceEntityCache, filters)
    }
    if (sortBy.nonEmpty) {
      links = sortLinks(links, sortBy)
    }
    val resultStats = ResultStats(overallLinkCount, links.size)
    links = links.slice(offset, offset + limit)
    linkEvaluationJsonResult(evaluationStats, linkingRule, links, resultStats, inputTaskLabel(linkTask))
  }

  private def schemaPathFunctions(linkSpec: LinkSpec): (String => Boolean, String => Boolean) = {
    linkSpec.inputPorts match {
      case FixedNumberOfInputs(Seq(FixedSchemaPort(sourceSchema), FixedSchemaPort(targetSchema))) =>
        val sourcePaths = sourceSchema.typedPaths.map(_.normalizedSerialization).toSet
        val targetPaths = targetSchema.typedPaths.map(_.normalizedSerialization).toSet
        (sourcePaths.contains, targetPaths.contains)
      case _ =>
        def alwaysTrue(path: String): Boolean = {
          true
        }
        (alwaysTrue, alwaysTrue)
    }
  }

  private def retrieveEvaluationLinks(linkTask: ProjectTask[LinkSpec])
                                     (implicit userContext: UserContext): Seq[EvaluatedLinkWithDecision] = {
    val evaluationActivity = linkTask.activity[EvaluateLinkingActivity]
    val referenceEntityCache = linkTask.activity[ReferenceEntitiesCache].value()
    if (evaluationActivity.control.status.get.isEmpty) {
      evaluationActivity.control.startBlocking()
    }
    for(link <- evaluationActivity.value().links) yield {
      val evaluatedLink =
        link match {
          case evaluatedLink: EvaluatedLink =>
            evaluatedLink
          case link: Link =>
            link.entities match {
              case Some(entities) =>
                DetailedEvaluator(linkTask.data.rule, entities)
              case None =>
                throw new IllegalArgumentException("Evaluation links are missing entities.")
            }
        }
      evaluatedLink.withDecision(referenceEntityCache.linkDecision(link))
    }
  }

  private def retrieveReferenceLinksSafe(linkTask: ProjectTask[LinkSpec]): Seq[EvaluatedLinkWithDecision] = {
    val referenceEntityCache = linkTask.activity[ReferenceEntitiesCache].value()
    val DPair(sourceEntitySchema, targetEntitySchema) = linkTask.data.entityDescriptions
    for(link <- referenceEntityCache.toReferenceLinksSafe(sourceEntitySchema, targetEntitySchema)) yield {
      val evaluatedLink = DetailedEvaluator(linkTask.data.rule, link.linkEntities)
      evaluatedLink.withDecision(link.decision)
    }
  }

  /** Labels of the source and target input tasks. */
  private def inputTaskLabel(linkingTask: ProjectTask[LinkSpec])
                            (implicit userContext: UserContext): (String, String) = {
    val project = linkingTask.project
    val sourceId = linkingTask.data.source.inputId
    val targetId = linkingTask.data.target.inputId
    (
      project.anyTaskOption(sourceId).map(_.fullLabel).getOrElse(sourceId),
      project.anyTaskOption(targetId).map(_.fullLabel).getOrElse(targetId)
    )
  }

  /**
    * Result statistics.
    * @param overallLinkCount The overall link count for the requested links before any filters are applied.
    * @param filteredLinkCount The link count after all filters (text, link decision etc.) have been applied.
    */
  private case class ResultStats(overallLinkCount: Int, filteredLinkCount: Int)

  private object ResultStats {
    implicit val resultStatsFormat: Format[ResultStats] = Json.format[ResultStats]
  }

  private def linkEvaluationJsonResult(evaluationActivityStats: Option[LinkRuleEvaluationStats],
                                       linkingRule: LinkageRule,
                                       links: Seq[Link],
                                       resultStats: ResultStats,
                                       inputTaskLabels: (String, String))
                                      (implicit writeContext: WriteContext[JsValue]) = {
    val linksJson = links.map(LinkJsonFormat.write)
    Ok(Json.obj(
      "links" -> linksJson,
      "linkRule" -> JsonSerialization.toJson(linkingRule),
      "resultStats" -> Json.toJson(resultStats),
      "evaluationActivityStats" -> evaluationActivityStats.map(stats => Json.toJson(stats)),
      "metaData" -> Json.obj(
        "sourceInputLabel" -> inputTaskLabels._1,
        "targetInputLabel" -> inputTaskLabels._2
      )
    ))
  }

  private def linkEvaluationStats(evaluationResult: Linking): LinkRuleEvaluationStats = {
    LinkRuleEvaluationStats(
      evaluationResult.statistics.entityCount.source,
      evaluationResult.statistics.entityCount.target,
      evaluationResult.links.size
    )
  }

  private def searchStringFromLink(link: Link, usedSourcePath: String => Boolean, usedTargetPath: String => Boolean): String = {
    val valueString = link.entities.map(e => s" ${searchStringFromEntity(e.source, usedSourcePath)} ${searchStringFromEntity(e.target, usedTargetPath)}").getOrElse("")
    s"${link.source} ${link.target} $valueString".toLowerCase
  }

  private def searchStringFromEntity(entity: Entity, usedPath: String => Boolean): String = {
    entity.schema.typedPaths.zip(entity.values)
      .filter(pathAndValues => usedPath(pathAndValues._1.normalizedSerialization))
      .flatMap(_._2)
      .mkString(" ")
  }
}