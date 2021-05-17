package controllers.transform

import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.transform.TransformTaskApi._
import controllers.transform.transformTask.{ObjectValueSourcePathInfo, TransformUtils, ValueSourcePathInfo}
import controllers.util.ProjectUtils._
import controllers.util.SerializationUtils._
import org.silkframework.config.{MetaData, Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.entity.paths.{PathOperator, TypedPath, UntypedPath}
import org.silkframework.rule.TransformSpec.{TargetVocabularyListParameter, TargetVocabularyParameterType}
import org.silkframework.rule._
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException, ValidationError, ValidationException}
import org.silkframework.serialization.json.JsonParseException
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.util.{Identifier, IdentifierGenerator, Uri}
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workspace.activity.transform.TransformPathsCache
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json._
import play.api.mvc._

import java.util
import java.util.logging.{Level, Logger}
import javax.inject.Inject
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class TransformTaskApi @Inject() () extends InjectedController {

  private val log = Logger.getLogger(getClass.getName)

  def getTransformTask(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    serializeCompileTime[TransformTask](task, Some(project))
  }

  def putTransformTask(projectName: String, taskName: String, createOnly: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = getProject(projectName)
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val readContext: ReadContext = ReadContext()

    request.body match {
      case AnyContentAsFormUrlEncoded(v) =>
        val values = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)
        val input = DatasetSelection(values("source"), Uri.parse(values.getOrElse("sourceType", ""), prefixes),
          Restriction.custom(values.getOrElse("restriction", "")))
        val output = values.get("output").filter(_.nonEmpty).map(Identifier(_))
        val targetVocabularies = values.get("targetVocabularies") match {
          case Some(v) => TargetVocabularyParameterType.fromString(v)
          case None => TargetVocabularyListParameter(Seq.empty)
        }

        project.tasks[TransformSpec].find(_.id.toString == taskName) match {
          //Update existing task
          case Some(oldTask) if !createOnly =>
            val updatedTransformSpec = oldTask.data.copy(selection = input, output = output, targetVocabularies = targetVocabularies)
            project.updateTask(taskName, updatedTransformSpec)
          //Create new task with no rule
          case _ =>
            val rule = RootMappingRule(rules = MappingRules.empty)
            val transformSpec = TransformSpec(input, rule, output, None, targetVocabularies)
            project.addTask(taskName, transformSpec, MetaData(MetaData.labelFromId(taskName)))
        }

        Ok
      case _ =>
        catchExceptions {
          deserializeCompileTime[TransformTask]() { task =>
            project.updateTask(task.id, task.data, Some(task.metaData))
            Ok
          }
        }
    }
  }

  def deleteTransformTask(projectName: String, taskName: String, removeDependentTasks: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = getProject(projectName)
    project.removeAnyTask(taskName, removeDependentTasks)

    Ok
  }

  def getRules(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    serializeCompileTime(task.data.mappingRule, Some(project))
  }

  def putRules(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val resources: ResourceManager = project.resources
    implicit val readContext: ReadContext = ReadContext(resources, prefixes)

    catchExceptions {
      task.synchronized {
        deserializeCompileTime[RootMappingRule]() { updatedRules =>
          //Update transformation task
          val updatedTask = task.data.copy(mappingRule = updatedRules)
          project.updateTask(taskName, updatedTask)
          Ok
        }
      }
    }
  }

  def getRule(projectName: String, taskName: String, ruleId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    processRule(task, ruleId) { rule =>
      serializeCompileTime(rule.operator.asInstanceOf[TransformRule], Some(project))
    }
  }

  def putRule(projectName: String, taskName: String, ruleId: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val resources: ResourceManager = project.resources
    implicit val readContext: ReadContext = ReadContext(resources, prefixes, identifierGenerator(task))

    task.synchronized {
      processRule(task, ruleId) { currentRule =>
        implicit val updatedRequest: Request[AnyContent] = updateJsonRequest(request, currentRule)
        deserializeCompileTime[TransformRule]() { updatedRule =>
          updateRule(currentRule.update(updatedRule))
          serializeCompileTime[TransformRule](updatedRule, Some(project))
        }
      }
    }
  }

  def deleteRule(projectName: String, taskName: String, rule: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    try {
      task.synchronized {
        val updatedTree = RuleTraverser(task.data.mappingRule).remove(rule)
        task.update(task.data.copy(mappingRule = updatedTree.operator.asInstanceOf[RootMappingRule]))
        Ok
      }
    } catch {
      case ex: NoSuchElementException =>
        ErrorResult(NotFoundException(ex))
    }
  }

  /**
    * Adds the rule provided in the request to the children of the specified transform task mapping rule.
    *
    * @param taskName    Transform task where the mapping rule should be added.
    * @param ruleName    The parent rule ID that the new rule should be added to as a child.
    * @param afterRuleId If specified then the new rule is added right after this rule. If not specified the new rule
    *                    is appended to the end of the list.
    * @return The newly created rule.
    */
  def appendRule(projectName: String,
                 taskName: String,
                 ruleName: String,
                 afterRuleId: Option[String] = None): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes
    task.synchronized {
      implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes, identifierGenerator(task))
      processRule(task, ruleName) { parentRule =>
        deserializeCompileTime[TransformRule]() { newChildRule =>
          addRuleToTransformTask(parentRule, newChildRule, afterRuleId)
        }
      }
    }
  }

  private def addRuleToTransformTask(parentRule: RuleTraverser,
                                     newChildRule: TransformRule,
                                     afterRuleId: Option[String])
                                    (implicit request: Request[AnyContent],
                                     task: ProjectTask[TransformSpec],
                                     userContext: UserContext,
                                     project: Project): Result = {
    if (task.data.nestedRuleAndSourcePath(newChildRule.id).isDefined) {
      throw new ValidationException(s"Rule with ID ${newChildRule.id} already exists!")
    }
    val children = parentRule.operator.children
    val newChildren = children.indexWhere(rule => afterRuleId.contains(rule.id.toString)) match {
      case afterRuleIdx: Int if afterRuleIdx >= 0 =>
        val (before, after) = children.splitAt(afterRuleIdx + 1)
        (before :+ newChildRule) ++ after // insert after specified rule
      case -1 => // append
        children :+ newChildRule
    }
    val updatedRule = parentRule.operator.withChildren(newChildren)
    updateRule(parentRule.update(updatedRule))
    serializeCompileTime(newChildRule, Some(project))
  }

  private def assignNewIdsAndLabelToRule(task: ProjectTask[TransformSpec],
                                 ruleToCopy: RuleTraverser): TransformRule = {
    implicit val idGenerator: IdentifierGenerator = identifierGenerator(task)
    ruleToCopy.operator match {
      case t: TransformRule =>
        val originalLabel = if(t.metaData.label.trim != "") t.metaData.label else t.target.map(_.propertyUri.toString).getOrElse("unlabeled")
        val newLabel = "Copy of " + originalLabel
        val transformRuleCopy = assignNewIdsToRule(t)
        transformRuleCopy.withMetaData(t.metaData.copy(label = newLabel))
      case other: Operator => throw new RuntimeException("Selected operator was not transform rule. Operator ID: " + other.id)
    }
  }

  private def assignNewIdsToRule(t: TransformRule)
                                (implicit idGenerator: IdentifierGenerator): TransformRule = {
    t match {
      case r: RootMappingRule =>
        val updatedMappingRules = assignNewIdsToMappingRules(r.rules)
        r.copy(id = idGenerator.generate(r.id), rules = updatedMappingRules)
      case c: ComplexMapping => c.copy(id = idGenerator.generate(c.id))
      case c: ComplexUriMapping => c.copy(id = idGenerator.generate(c.id))
      case d: DirectMapping => d.copy(id = idGenerator.generate(d.id))
      case o: ObjectMapping =>
        val updatedMappingRules = assignNewIdsToMappingRules(o.rules)
        o.copy(id = idGenerator.generate(o.id), rules = updatedMappingRules)
      case typeMapping: TypeMapping => assignNewIdsToRule(typeMapping)
      case uriMapping: UriMapping => assignNewIdsToRule(uriMapping)
    }
  }

  private def assignNewIdsToMappingRules(mappingRules: MappingRules)
                                        (implicit identifierGenerator: IdentifierGenerator): MappingRules = {
    mappingRules.copy(
      uriRule = mappingRules.uriRule.map(assignNewIdsToRule),
      typeRules = mappingRules.typeRules.map(assignNewIdsToRule),
      propertyRules = mappingRules.propertyRules.map(assignNewIdsToRule)
    )
  }

  private def assignNewIdsToRule(typeMapping: TypeMapping)
                                (implicit idGenerator: IdentifierGenerator): TypeMapping = {
    typeMapping.copy(id = idGenerator.generate(typeMapping.id))
  }

  private def assignNewIdsToRule(uriMapping: UriMapping)
                                (implicit idGenerator: IdentifierGenerator): UriMapping = {
    uriMapping match {
      case c: ComplexUriMapping =>
        c.copy(id = idGenerator.generate(c.id))
      case p: PatternUriMapping =>
        p.copy(id = idGenerator.generate(p.id))
    }
  }

  /** Converts a root mapping rule to an object mapping rule. */
  private def convertRootMappingRule(rule: TransformRule): TransformRule = {
    rule match {
      case RootMappingRule(rules, id, target, metaData) =>
        ObjectMapping(id, rules = rules, metaData = metaData, target = Some(target.copy(propertyUri = ROOT_COPY_TARGET_PROPERTY)))
      case other: TransformRule =>
        other
    }
  }

  /**
    * Copies a mapping rule from a source transform task to a target transform task.
    *
    * @param projectName   The target project where the rule is copied to.
    * @param taskName      The target transform task the rule is copied to.
    * @param ruleName      The target rule where the copied rule should be added as child.
    * @param sourceProject The project the source rule is copied from.
    * @param sourceTask    The source task the source rule is copied from.
    * @param sourceRule    The ID of the source rule that should be copied.
    * @param afterRuleId   An optional rule ID of one of the children of the parent rule after which the new rule should be
    *                      added.
    * @return The newly added rule.
    */
  def copyRule(projectName: String,
               taskName: String,
               ruleName: String,
               sourceProject: String,
               sourceTask: String,
               sourceRule: String,
               afterRuleId: Option[String] = None): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    val (_, fromTask) = getProjectAndTask[TransformSpec](sourceProject, sourceTask)
    implicit val prefixes: Prefixes = project.config.prefixes
    task.synchronized {
      implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes, identifierGenerator(task))
      processRule(fromTask, sourceRule) { ruleToCopy =>
        processRule(task, ruleName) { parentRule =>
          val newChildRule = convertRootMappingRule(assignNewIdsAndLabelToRule(task, ruleToCopy))
          addRuleToTransformTask(parentRule, newChildRule, afterRuleId)
        }
      }
    }
  }

  def reorderRules(projectName: String, taskName: String, ruleName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    task.synchronized {
      processRule(task, ruleName) { parentRule =>
        request.body.asJson match {
          case Some(json) =>
            val currentRules = parentRule.operator.asInstanceOf[TransformRule].rules
            val currentOrder = currentRules.propertyRules.map(_.id.toString).toList
            val newOrder = json.as[JsArray].value.map(_.as[JsString].value).toList
            if (newOrder.toSet == currentOrder.toSet) {
              val newPropertyRules =
                for (id <- newOrder) yield {
                  parentRule.operator.children.find(_.id == id).get
                }
              val newRules = currentRules.uriRule.toSeq ++ currentRules.typeRules ++ newPropertyRules
              updateRule(parentRule.update(parentRule.operator.withChildren(newRules)))
              Ok(JsArray(newPropertyRules.map(r => JsString(r.id))))
            } else {
              ErrorResult(BadUserInputException(s"Provided list $newOrder does not contain the same elements as current list $currentOrder."))
            }
          case None =>
            ErrorResult(UnsupportedMediaTypeException.supportedFormats("application/json."))
        }
      }
    }
  }

  /**
    * Processes a rule a catches relevant exceptions
    */
  private def processRule(task: Task[TransformSpec], ruleId: String)(processFunc: RuleTraverser => Result): Result = {
    RuleTraverser(task.data.mappingRule).find(ruleId) match {
      case Some(rule) =>
        catchExceptions(processFunc(rule))
      case None =>
        ErrorResult(NotFoundException(s"No rule with id '$ruleId' found!"))
    }
  }

  /**
    * Catches relevant exceptions and returns appropriate error codes.
    */
  private def catchExceptions(func: => Result): Result = {
    try {
      func
    } catch {
      case ex: BadUserInputException =>
        log.log(Level.FINE, "Invalid transformation rule", ex)
        ErrorResult.validation(BAD_REQUEST, ex.getMessage, ValidationError(ex.getMessage) :: Nil)
      case ex: ValidationException =>
        log.log(Level.INFO, "Invalid transformation rule", ex)
        ErrorResult.validation(BAD_REQUEST, "Invalid transformation rule", ex.errors)
      case ex: JsonParseException =>
        log.log(Level.INFO, "Invalid transformation rule JSON", ex)
        ErrorResult(BadUserInputException(ex))
      case ex: Exception =>
        log.log(Level.WARNING, "Failed process mapping rule", ex)
        ErrorResult.validation(INTERNAL_SERVER_ERROR, "Failed to process mapping rule", ValidationError("Error in back end: " + ex.getMessage) :: Nil)
    }
  }

  private def identifierGenerator(transformTask: Task[TransformSpec]): IdentifierGenerator = {
    TransformSpec.identifierGenerator(transformTask.data)
  }

  private def updateJsonRequest(request: Request[AnyContent], rule: RuleTraverser): Request[AnyContent] = {
    request.body.asJson match {
      case Some(requestJson) =>
        val ruleJson = toJson(rule.operator.asInstanceOf[TransformRule]).as[JsObject]
        val updatedJson = ruleJson.deepMerge(requestJson.as[JsObject])
        request.map(_ => AnyContentAsJson(updatedJson))
      case None => request
    }
  }

  private def updateRule(ruleTraverser: RuleTraverser)
                        (implicit task: ProjectTask[TransformSpec],
                         userContext: UserContext): Unit = {
    val updatedRoot = ruleTraverser.root.operator.asInstanceOf[RootMappingRule]
    val updatedTask = task.data.copy(mappingRule = updatedRoot)
    task.project.updateTask(task.id, updatedTask)
  }

  def reloadTransformCache(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    task.activity[TransformPathsCache].control.reset()
    task.activity[TransformPathsCache].control.start()
    Ok
  }

  def executeTransformTask(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val activity = task.activity[ExecuteTransform].control
    activity.start()
    Ok
  }

  /**
    * Transform entities bundled with the request according to the transformation task.
    *
    * @return If no sink is specified in the request then return results in N-Triples format with the response,
    *         else write triples to defined data sink.
    */
  def postTransformInput(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val (_, task) = projectAndTask(projectName, taskName)
    request.body match {
      case AnyContentAsXml(xmlRoot) =>
        implicit val (resourceManager, _) = createInMemoryResourceManagerForResources(xmlRoot, projectName, withProjectResources = true)
        val dataSource = createDataSource(xmlRoot, None)
        val (model, entitySink) = createEntitySink(xmlRoot)
        executeTransform(task, entitySink, dataSource, errorEntitySinkOpt = None)
        val acceptedContentType = request.acceptedTypes.headOption.map(_.toString()).getOrElse("application/n-triples")
        result(model, acceptedContentType, "Data transformed successfully!")
      case _ =>
        throw UnsupportedMediaTypeException.supportedFormats("application/xml")
    }
  }

  private def executeTransform(task: ProjectTask[TransformSpec],
                               entitySink: EntitySink,
                               dataSource: DataSource,
                               errorEntitySinkOpt: Option[EntitySink])
                              (implicit userContext: UserContext): Unit = {
    implicit val prefixes: Prefixes = task.project.config.prefixes
    val transform = new ExecuteTransform(task, (_) => dataSource, (_) => entitySink, (_) => errorEntitySinkOpt)
    Activity(transform).startBlocking()
  }

  private def projectAndTask(projectName: String, taskName: String)
                            (implicit userContext: UserContext): (Project, ProjectTask[TransformSpec]) = {
    getProjectAndTask[TransformSpec](projectName, taskName)
  }

  /** Returns an array of string representations of the available source paths. */
  def valueSourcePaths(projectName: String,
                       taskName: String,
                       ruleId: String,
                       maxDepth: Int,
                       unusedOnly: Boolean,
                       usedOnly: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    if(unusedOnly && usedOnly) {
      throw BadUserInputException("Only one of the following parameters can be true, but both of them were true: unusedOnly, usedOnly")
    }
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes
    Ok(Json.toJson(typedRuleValuePaths(task, ruleId, maxDepth, unusedOnly, usedOnly).map(_.serialize())))
  }

  /** Returns all available paths of max. depth for the specific transform rule.
    *
    * @param maxDepth   Max depth/hops of the returned paths.
    * @param objectInfo If additional information, e.g. stats, sub-paths, should be attached.
    */
  def valueSourcePathsFullInfo(projectId: String,
                               transformTaskId: String,
                               mappingRuleId: String,
                               maxDepth: Int,
                               objectInfo: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    implicit val (project, transformTask) = getProjectAndTask[TransformSpec](projectId, transformTaskId)
    implicit val prefixes: Prefixes = project.config.prefixes
    val dataSourceCharacteristicsOpt = TransformUtils.datasetCharacteristics(transformTask)
    val typedPaths = typedRuleValuePaths(transformTask, mappingRuleId, maxDepth)
    val usedPaths = typedRuleValuePaths(transformTask, mappingRuleId, maxDepth, usedOnly = true).toSet
    val objectPathInfos: Map[UntypedPath, ObjectValueSourcePathInfo] = dataSourceCharacteristicsOpt match {
      case Some(characteristics) if characteristics.supportedPathExpressions.multiHopPaths && objectInfo =>
        val objectPaths = typedPaths.filter(_.valueType.id == "UriValueType").map(_.toUntypedPath).toSet
        val isRdfInput = TransformUtils.isRdfInput(transformTask)
        if(objectPaths.isEmpty) {
          Map.empty
        } else if (isRdfInput) {
          // TODO: Fetch RDF infos
          Map.empty
        } else {
          val maxObjectPathLength = objectPaths.map(_.operators.size).max
          val additionalHopPath = if(maxDepth < Int.MaxValue || maxObjectPathLength == maxDepth) {
            // Need to fetch paths with one more hop in order to calculate stats
            typedRuleValuePaths(transformTask, mappingRuleId, maxDepth + 1)
          } else {
            typedPaths
          }
          val subPathMap = mutable.HashMap[UntypedPath, ArrayBuffer[TypedPath]]()
          additionalHopPath filter { tp => tp.operators.size > 1 } foreach { tp =>
            val parentPath = UntypedPath(tp.operators.dropRight(1))
            if(objectPaths.contains(parentPath)) {
              subPathMap.getOrElseUpdate(parentPath, new ArrayBuffer[TypedPath]()).append(tp)
            }
          }
          def relativePathString(tp: TypedPath) = UntypedPath(tp.operators.takeRight(1)).serialize()
          subPathMap.mapValues { typedPaths =>
            val (objectPaths, dataPaths) = typedPaths.partition(_.valueType.id == "UriValueType")
            ObjectValueSourcePathInfo(
              dataTypeSubPaths = dataPaths.map(relativePathString),
              objectSubPaths = objectPaths.map(relativePathString)
            )
          }.toMap
        }
      case _ => Map.empty
    }
    val valuePathInfos = typedPaths map { tp =>
      val pathType = if(tp.valueType.id == "UriValueType") "object" else "value"
      ValueSourcePathInfo(tp.serialize(), pathType, alreadyMapped = usedPaths.contains(tp), objectPathInfos.get(tp.toUntypedPath))
    }
    Ok(Json.toJson(valuePathInfos))
  }

  private def typedRuleValuePaths(task: ProjectTask[TransformSpec],
                                  ruleId: String,
                                  maxDepth: Int,
                                  unusedOnly: Boolean = false,
                                  usedOnly: Boolean = false)
                                 (implicit userContext: UserContext,
                                  prefixes: Prefixes): IndexedSeq[TypedPath] = {
    assert(!unusedOnly || !usedOnly, "Only one of the following parameters can be true, but both of them were true: unusedOnly, usedOnly")
    task.nestedRuleAndSourcePath(ruleId) match {
      case Some((_, sourcePath)) =>
        val matchingPaths = relativePathsFromPathsCache(maxDepth, sourcePath, task)
        if(unusedOnly || usedOnly) {
          val sourcePaths = usedSourcePaths(ruleId, maxDepth, task)
          val filterFn: UntypedPath => Boolean = if(unusedOnly) path => !sourcePaths.contains(path) else sourcePaths.contains
          matchingPaths filter { path =>
            filterFn(path.asUntypedPath)
          }
        } else {
          matchingPaths
        }
      case None =>
        throw NotFoundException("No rule found with ID " + ruleId)
    }
  }

  // Get the relative paths matching the path prefix with the max. depth length from the transform path cache.
  private def relativePathsFromPathsCache(maxDepth: Int,
                                          pathPrefix: List[PathOperator],
                                          task: ProjectTask[TransformSpec])
                                         (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    val pathCache = task.activity[TransformPathsCache]
    pathCache.control.waitUntilFinished()
    val isRdfInput = TransformUtils.isRdfInput(task)
    val cachedPaths = pathCache.value().fetchCachedPaths(task, pathPrefix.nonEmpty && isRdfInput)
    cachedPaths filter { p =>
      val pathSize = p.operators.size
      isRdfInput ||
        p.operators.startsWith(pathPrefix) &&
          pathSize > pathPrefix.size &&
          pathSize - pathPrefix.size <= maxDepth
    } map { p =>
      if (isRdfInput) {
        p
      } else {
        TypedPath.removePathPrefix(p, UntypedPath(pathPrefix))
      }
    }
  }

  /** The relative source path that are already used in the specified mapping rule. */
  private def usedSourcePaths(ruleId: String, maxDepth: Int,
                              task: ProjectTask[TransformSpec]): Set[UntypedPath] = {
    task.data.valueSourcePaths(ruleId, maxDepth).toSet ++
      task.data.objectSourcePaths(ruleId).toSet
  }
}

object TransformTaskApi {

  // The property that is set when copying a root mapping rule that will be converted into an object mapping rule
  final val ROOT_COPY_TARGET_PROPERTY = "urn:temp:child"
}

// Peak API
case class PeakResults(sourcePaths: Option[Seq[Seq[String]]], results: Option[Seq[PeakResult]], status: PeakStatus)

case class PeakStatus(id: String, msg: String)

case class PeakResult(sourceValues: Seq[Seq[String]], transformedValues: Seq[String])
