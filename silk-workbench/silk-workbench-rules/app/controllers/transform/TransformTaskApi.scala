package controllers.transform

import java.util.logging.{Level, Logger}

import controllers.util.ProjectUtils._
import controllers.util.SerializationUtils._
import org.silkframework.config.{Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.entity.{Entity, EntitySchema, Path, Restriction}
import org.silkframework.rule._
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.rule.vocab.{VocabularyClass, VocabularyProperty}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.{ValidationError, ValidationException}
import org.silkframework.serialization.json.{JsonParseException, JsonSerializers}
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.util.{Identifier, IdentifierGenerator, Uri}
import org.silkframework.workbench.utils.JsonError
import org.silkframework.workspace.activity.transform.{TransformPathsCache, VocabularyCache}
import org.silkframework.workspace.{Project, ProjectTask, User}
import play.api.libs.json._
import play.api.mvc._

import scala.util.control.NonFatal

class TransformTaskApi extends Controller {
  implicit private val peakStatusWrites = Json.writes[PeakStatus]
  implicit private val peakResultWrites = Json.writes[PeakResult]
  implicit private val peakResultsWrites = Json.writes[PeakResults]
  // Max number of exceptions before aborting the mapping preview call
  final val MAX_TRANSFORMATION_PREVIEW_EXCEPTIONS: Int = 50
  // The number of transformation preview results that should be returned by the REST API
  final val TRANSFORMATION_PREVIEW_LIMIT: Int = 3
  // Maximum number of empty transformation results to skip during the mapping preview calculation
  final val MAX_TRANSFORMATION_PREVIEW_SKIP_EMPTY_RESULTS: Int = 500
  // Max number of entities to examine for the mapping preview
  final val MAX_TRY_ENTITIES_DEFAULT: Int = MAX_TRANSFORMATION_PREVIEW_EXCEPTIONS + TRANSFORMATION_PREVIEW_LIMIT + MAX_TRANSFORMATION_PREVIEW_SKIP_EMPTY_RESULTS
  final val NOT_SUPPORTED_STATUS_MSG = "not supported"

  private val log = Logger.getLogger(getClass.getName)

  def getTransformTask(projectName: String, taskName: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes

    serializeCompileTime[Task[TransformSpec]](task)
  }

  def putTransformTask(project: String, task: String): Action[AnyContent] = Action { implicit request => {
    val proj = User().workspace.project(project)
    implicit val prefixes = proj.config.prefixes
    implicit val readContext = ReadContext()

    request.body match {
      case AnyContentAsFormUrlEncoded(v) =>
        val values = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)
        val input = DatasetSelection(values("source"), Uri.parse(values.getOrElse("sourceType", ""), prefixes), Restriction.custom(values.getOrElse("restriction", "")))
        val outputs = values.get("output").filter(_.nonEmpty).map(Identifier(_)).toSeq
        val targetVocabularies = values.get("targetVocabularies").toSeq.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)

        proj.tasks[TransformSpec].find(_.id == task) match {
          //Update existing task
          case Some(oldTask) =>
            val updatedTransformSpec = oldTask.data.copy(selection = input, outputs = outputs, targetVocabularies = targetVocabularies)
            proj.updateTask(task, updatedTransformSpec)
          //Create new task with no rule
          case None =>
            val transformSpec = TransformSpec(input, RootMappingRule("root", MappingRules.empty), outputs, Seq.empty, targetVocabularies)
            proj.updateTask(task, transformSpec)
        }

        Ok
      case _ =>
        catchExceptions {
          deserializeCompileTime[Task[TransformSpec]]() { task =>
            proj.updateTask(task.id, task.data)
            Ok
          }
        }
    }
  }
  }

  def deleteTransformTask(projectName: String, taskName: String, removeDependentTasks: Boolean): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    project.removeAnyTask(taskName, removeDependentTasks)

    Ok
  }

  def getRules(projectName: String, taskName: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes

    serializeCompileTime(task.data.mappingRule)
  }

  def putRules(projectName: String, taskName: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    implicit val readContext = ReadContext(resources, prefixes)

    catchExceptions {
      deserializeCompileTime[RootMappingRule]() { updatedRules =>
        //Update transformation task
        val updatedTask = task.data.copy(mappingRule = updatedRules)
        project.updateTask(taskName, updatedTask)
        Ok
      }
    }
  }

  def getRule(projectName: String, taskName: String, ruleId: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes

    processRule(task, ruleId) { rule =>
      serializeCompileTime(rule.operator.asInstanceOf[TransformRule])
    }
  }

  def putRule(projectName: String, taskName: String, ruleId: String): Action[AnyContent] = Action { request =>
    implicit val project = User().workspace.project(projectName)
    implicit val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    implicit val readContext = ReadContext(resources, prefixes, identifierGenerator(task))

    processRule(task, ruleId) { currentRule =>
      implicit val updatedRequest = updateJsonRequest(request, currentRule)
      deserializeCompileTime[TransformRule]() { updatedRule =>
        updateRule(currentRule.update(updatedRule))
        serializeCompileTime[TransformRule](updatedRule)
      }
    }
  }

  def deleteRule(projectName: String, taskName: String, rule: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes

    try {
      val updatedTree = RuleTraverser(task.data.mappingRule).remove(rule)
      task.update(task.data.copy(mappingRule = updatedTree.operator.asInstanceOf[RootMappingRule]))
      Ok
    } catch {
      case ex: NoSuchElementException =>
        NotFound(JsonError(ex.getMessage))
    }
  }

  def appendRule(projectName: String, taskName: String, ruleName: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    implicit val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes
    implicit val readContext = ReadContext(project.resources, project.config.prefixes, identifierGenerator(task))

    processRule(task, ruleName) { parentRule =>
      deserializeCompileTime[TransformRule]() { newChildRule =>
        val updatedRule = parentRule.operator.withChildren(parentRule.operator.children :+ newChildRule)
        updateRule(parentRule.update(updatedRule))
        serializeCompileTime(newChildRule)
      }
    }
  }

  def reorderRules(projectName: String, taskName: String, ruleName: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    implicit val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes

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
            BadRequest(JsonError(s"Provided list $newOrder does not contain the same elements as current list $currentOrder."))
          }
        case None =>
          NotAcceptable(JsonError("Expected application/json."))
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
        NotFound(JsonError(s"No rule with id '$ruleId' found!"))
    }
  }

  /**
    * Catches relevant exceptions and returns appropriate error codes.
    */
  private def catchExceptions(func: => Result): Result = {
    try {
      func
    } catch {
      case ex: ValidationException =>
        log.log(Level.INFO, "Invalid transformation rule", ex)
        BadRequest(JsonError("Invalid transformation rule", ex.errors))
      case ex: JsonParseException =>
        log.log(Level.INFO, "Invalid transformation rule JSON", ex)
        BadRequest(JsonError(ex))
      case ex: Exception =>
        log.log(Level.WARNING, "Failed process mapping rule", ex)
        InternalServerError(JsonError("Failed to process mapping rule", ValidationError("Error in back end: " + ex.getMessage) :: Nil))
    }
  }

  private def identifierGenerator(transformTask: Task[TransformSpec]): IdentifierGenerator = {
    val identifierGenerator = new IdentifierGenerator()
    for(id <- RuleTraverser(transformTask.data.mappingRule).iterateAllChildren.map(_.operator.id)) {
      identifierGenerator.add(id)
    }
    identifierGenerator
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

  private def updateRule(ruleTraverser: RuleTraverser)(implicit task: ProjectTask[TransformSpec]): Unit = {
    val updatedRoot = ruleTraverser.root.operator.asInstanceOf[RootMappingRule]
    val updatedTask = task.data.copy(mappingRule = updatedRoot)
    task.project.updateTask(task.id, updatedTask)
  }

  def reloadTransformCache(projectName: String, taskName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    task.activity[TransformPathsCache].control.reset()
    task.activity[TransformPathsCache].control.start()
    Ok
  }

  def executeTransformTask(projectName: String, taskName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val activity = task.activity[ExecuteTransform].control
    activity.start()
    Ok
  }

  /**
    * Returns a JSON array of candidate types, either from the vocabulary cache or the matching cache of the transform task.
    *
    * @param projectName The name of the project
    * @param taskName    The name of the transform task
    */
  def typeCandidates(projectName: String, taskName: String): Action[AnyContent] = Action {
    val (_, task) = projectAndTask(projectName, taskName)
    val typeCompletion = new AutoCompletionApi().retrieveTypeCompletions(task)
    Ok(JsArray(typeCompletion.map(_.toJson)))
  }

  /**
    * Returns all properties that are in the domain of the given class or one of its super classes.
    * @param projectName Name of project
    * @param taskName    Name of task
    * @param classUri    Class URI
    */
  def propertiesByType(projectName: String, taskName: String, classUri: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    val (vocabularyProps, _) = vocabularyPropertiesByType(taskName, project, classUri, addBackwardRelations = false)
    serializeIterableCompileTime(vocabularyProps, containerName = Some("Properties"))
  }

  /**
    * Returns all properties that are directly defined on a class or one of its parent classes.
    * @param classUri The class we want to have the relations for.
    * @param addBackwardRelations Specifies if backward relations should be added
    * @return Tuple of (forward properties, backward properties)
    */
  private def vocabularyPropertiesByType(taskName: String,
                                         project: Project,
                                         classUri: String,
                                         addBackwardRelations: Boolean): (Seq[VocabularyProperty], Seq[VocabularyProperty]) = {
    val task = project.task[TransformSpec](taskName)
    val vocabularies = task.activity[VocabularyCache].value
    val vocabularyClasses = vocabularies.flatMap(v => v.getClass(classUri).map(c => (v, c)))

    def filterProperties(propFilter: (VocabularyProperty, List[String]) => Boolean): Seq[VocabularyProperty] = {
      val props = for ((vocabulary, vocabularyClass) <- vocabularyClasses) yield {
        val classes = (vocabularyClass.info.uri :: vocabularyClass.parentClasses.toList).distinct
        val propsByAnyClass = vocabulary.properties.filter(propFilter(_, classes))
        propsByAnyClass
      }
      props.flatten
    }

    val forwardProperties = filterProperties((prop, classes) => prop.domain.exists(vc => classes.contains(vc.info.uri)))
    val backwardProperties = filterProperties((prop, classes) => addBackwardRelations && prop.range.exists(vc => classes.contains(vc.info.uri)))
    (forwardProperties, backwardProperties)
  }

  case class ClassRelations(forwardRelations: Seq[Relation], backwardRelations: Seq[Relation])

  case class Relation(property: VocabularyProperty, targetClass: VocabularyClass)

  /** Json serializers */
  implicit private val writeContext = WriteContext[JsValue]()
  implicit private object vocabularyClassFormat extends Writes[VocabularyClass] {
    override def writes(vocabularyClass: VocabularyClass): JsValue = {
      JsonSerializers.VocabularyClassJsonFormat.write(vocabularyClass)
    }
  }
  implicit private object vocabularyPropertyFormat extends Writes[VocabularyProperty] {
    override def writes(vocabularyProperty: VocabularyProperty): JsValue = {
      JsonSerializers.VocabularyPropertyJsonFormat.write(vocabularyProperty)
    }
  }
  implicit private val relationFormat = Json.writes[Relation]
  implicit private val classRelationsFormat = Json.writes[ClassRelations]

  // Depending on the forward switch either the range or the domain is taken for the classUri.
  private def vocabularyPropertyToRelation(vocabularyProperty: VocabularyProperty, forward: Boolean): Relation = {
    val targetClass = if(forward) { vocabularyProperty.range } else { vocabularyProperty.domain }
    assert(targetClass.isDefined, "No target class defined for relation property " + vocabularyProperty.info.uri)
    Relation(vocabularyProperty, targetClass.get)
  }

  def relationsOfType(projectName: String, taskName: String, classUri: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    // Filter only object properties
    val (forwardProperties, backwardProperties) = vocabularyPropertiesByType(taskName, project, classUri, addBackwardRelations = true)
    val forwardObjectProperties = forwardProperties.filter(vp => vp.range.isDefined && vp.domain.isDefined)
    val f = forwardObjectProperties map (fp => vocabularyPropertyToRelation(fp, forward = true))
    val b = backwardProperties map (bp => vocabularyPropertyToRelation(bp, forward = false))
    val classRelations = ClassRelations(f, b)
    Ok(Json.toJson(classRelations))
  }

  /**
    * Transform entities bundled with the request according to the transformation task.
    *
    * @return If no sink is specified in the request then return results in N-Triples format with the response,
    *         else write triples to defined data sink.
    */
  def postTransformInput(projectName: String, taskName: String): Action[AnyContent] = Action { request =>
    val (_, task) = projectAndTask(projectName, taskName)
    request.body match {
      case AnyContentAsXml(xmlRoot) =>
        implicit val resourceManager = createInmemoryResourceManagerForResources(xmlRoot)
        val dataSource = createDataSource(xmlRoot, None)
        val (model, entitySink) = createEntitySink(xmlRoot)
        executeTransform(task, entitySink, dataSource, errorEntitySinkOpt = None)
        val acceptedContentType = request.acceptedTypes.headOption.map(_.toString()).getOrElse("application/n-triples")
        result(model, acceptedContentType, "Data transformed successfully!")
      case _ =>
        UnsupportedMediaType("Only XML supported")
    }
  }

  private def executeTransform(task: ProjectTask[TransformSpec],
                               entitySink: EntitySink,
                               dataSource: DataSource,
                               errorEntitySinkOpt: Option[EntitySink]): Unit = {
    val transform = new ExecuteTransform(dataSource, task.data, Seq(entitySink))
    Activity(transform).startBlocking()
  }

  private def projectAndTask(projectName: String, taskName: String) = {
    getProjectAndTask[TransformSpec](projectName, taskName)
  }

  /** Get sample source and transformed values */
  def peakIntoTransformRule(projectName: String,
                            taskName: String,
                            ruleName: String): Action[AnyContent] = Action { request =>
    val limit = request.getQueryString("limit").map(_.toInt).getOrElse(TRANSFORMATION_PREVIEW_LIMIT)
    val maxTryEntities = request.getQueryString("maxTryEntities").map(_.toInt).getOrElse(MAX_TRY_ENTITIES_DEFAULT)
    val (project, task) = projectAndTask(projectName, taskName)
    val transformTask = task.data
    val inputTask = transformTask.selection.inputId
    implicit val prefixes = project.config.prefixes
    project.anyTask(inputTask).data match {
      case dataset: Dataset =>
        dataset.source match {
          case peakDataSource: PeakDataSource =>
            val rule = task.data.mappingRule.rules.find(_.id.toString == ruleName).getOrElse(
              throw new IllegalArgumentException(s"Transform task $taskName in project $projectName has no transformation rule $ruleName! Valid rule names: "
                  + task.data.mappingRule.rules.map(_.id).mkString(", "))
            )
            val entityDescription = oneRuleEntitySchema(transformTask, rule)
            try {
              val exampleEntities = peakDataSource.peak(entityDescription, maxTryEntities)
              generateMappingPreviewResponse(rule, exampleEntities, limit)
            } catch {
              case pe: PeakException =>
                Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, "Input dataset task " + inputTask.toString +
                    " of type " + dataset.plugin.label +
                    " raised following issue:" + pe.msg))))
            }
          case _ =>
            Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, "Input dataset task " + inputTask.toString +
                " of type " + dataset.plugin.label +
                " does not support transformation preview!"))))
        }
      case _: TransformSpec =>
        Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, "Input task " + inputTask.toString +
            " is not a Dataset. Currently mapping preview is only supported for dataset inputs."))))
    }
  }

  // Generate the HTTP response for the mapping transformation preview
  private def generateMappingPreviewResponse(rule: TransformRule,
                                             exampleEntities: Traversable[Entity],
                                             limit: Int)
                                            (implicit prefixes: Prefixes) = {
    val (tryCounter, errorCounter, errorMessage, sourceAndTargetResults) = collectTransformationExamples(rule, exampleEntities, limit)
    if (sourceAndTargetResults.nonEmpty) {
      Ok(Json.toJson(PeakResults(Some(rule.paths.map(serializePath)), Some(sourceAndTargetResults),
        status = PeakStatus("success", ""))))
    } else if (errorCounter > 0) {
      Ok(Json.toJson(PeakResults(Some(rule.paths.map(serializePath)), Some(sourceAndTargetResults),
        status = PeakStatus("empty with exceptions",
          s"Transformation result was always empty or exceptions occurred. $tryCounter processed and $errorCounter exceptions occurred. " +
              "First exception: " + errorMessage))))
    } else {
      Ok(Json.toJson(PeakResults(Some(rule.paths.map(serializePath)), Some(sourceAndTargetResults),
        status = PeakStatus("empty", s"Transformation result was always empty. Processed first $tryCounter entities."))))
    }
  }

  /**
    *
    * @param rule            The transformation rule to execute on the example entities.
    * @param exampleEntities Entities to try executing the tranform rule on
    * @param limit           Limit of examples to return
    * @return
    */
  def collectTransformationExamples(rule: TransformRule, exampleEntities: Traversable[Entity], limit: Int): (Int, Int, String, Seq[PeakResult]) = {
    // Number of examples collected
    var exampleCounter = 0
    // Number of exceptions occurred
    var errorCounter = 0
    // Number of example entities tried
    var tryCounter = 0
    // Record the first error message
    var errorMessage: String = ""
    val sourceAndTargetResults = (for (entity <- exampleEntities
                                       if exampleCounter < limit) yield {
      tryCounter += 1
      try {
        val transformResult = rule(entity)
        if (transformResult.nonEmpty) {
          val result = Some(PeakResult(entity.values, transformResult))
          exampleCounter += 1
          result
        } else {
          None
        }
      } catch {
        case NonFatal(ex) =>
          errorCounter += 1
          if (errorMessage.isEmpty) {
            errorMessage = ex.getClass.getSimpleName + ": " + Option(ex.getMessage).getOrElse("")
          }
          None
      }
    }).toSeq.flatten
    (tryCounter, errorCounter, errorMessage, sourceAndTargetResults)
  }

  private def serializePath(path: Path)
                           (implicit prefixes: Prefixes): Seq[String] = {
    path.operators.map { op =>
      op.serialize
    }
  }

  private def oneRuleEntitySchema(transformTask: TransformSpec,
                                  rule: TransformRule) = {
    EntitySchema(
      typeUri = transformTask.selection.typeUri,
      typedPaths = rule.paths.distinct.
          map(_.asStringTypedPath).toIndexedSeq,
      filter = transformTask.selection.restriction
    )
  }
}

case class PeakResults(sourcePaths: Option[Seq[Seq[String]]], results: Option[Seq[PeakResult]], status: PeakStatus)

case class PeakStatus(id: String, msg: String)

case class PeakResult(sourceValues: Seq[Seq[String]], transformedValues: Seq[String])