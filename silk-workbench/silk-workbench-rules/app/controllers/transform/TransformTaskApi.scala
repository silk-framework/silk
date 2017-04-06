package controllers.transform

import java.util.logging.{Level, Logger}

import controllers.util.ProjectUtils._
import org.silkframework.config.Prefixes
import org.silkframework.dataset.{DataSource, Dataset, EntitySink, PeakDataSource}
import org.silkframework.entity.{Entity, EntitySchema, Path, Restriction}
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformRule, TransformSpec}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.runtime.validation.{ValidationError, ValidationException, ValidationWarning}
import org.silkframework.util.{CollectLogs, Identifier, Uri}
import org.silkframework.workbench.utils.JsonError
import org.silkframework.workspace.activity.transform.TransformPathsCache
import org.silkframework.workspace.{ProjectTask, User}
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.mvc.{Action, AnyContent, AnyContentAsXml, Controller}

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

  private val log = Logger.getLogger(getClass.getName)

  def putTransformTask(project: String, task: String): Action[AnyContent] = Action { implicit request => {
    val values = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)

    val proj = User().workspace.project(project)
    implicit val prefixes = proj.config.prefixes

    val input = DatasetSelection(values("source"), Uri.parse(values.getOrElse("sourceType", ""), prefixes), Restriction.custom(values("restriction")))
    val outputs = values.get("output").filter(_.nonEmpty).map(Identifier(_)).toSeq
    val targetVocabularies = values.get("targetVocabularies").toSeq.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)

    proj.tasks[TransformSpec].find(_.id == task) match {
      //Update existing task
      case Some(oldTask) => {
        val updatedTransformSpec = oldTask.data.copy(selection = input, outputs = outputs, targetVocabularies = targetVocabularies)
        proj.updateTask(task, updatedTransformSpec)
      }
      //Create new task with no rule
      case None => {
        val transformSpec = TransformSpec(input, Seq.empty, outputs, Seq.empty, targetVocabularies)
        proj.updateTask(task, transformSpec)
      }
    }
    Ok
  }
  }

  def deleteTransformTask(projectName: String, taskName: String, removeDependentTasks: Boolean): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
     project.removeAnyTask( taskName, removeDependentTasks)

    Ok
  }

  def getRules(projectName: String, taskName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes

    Ok(<TransformRules>
      {task.data.rules.map(XmlSerialization.toXml[TransformRule])}
    </TransformRules>)
  }

  def putRules(projectName: String, taskName: String): Action[AnyContent] = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    implicit val readContext = ReadContext(resources, prefixes)

    request.body.asXml match {
      case Some(xml) =>
        try {
          //Parse transformation rules
          val updatedRules = (xml \ "TransformRule").map(XmlSerialization.fromXml[TransformRule])
          //Update transformation task
          val updatedTask = task.data.copy(rules = updatedRules)
          project.updateTask(taskName, updatedTask)
          Ok
        } catch {
          case ex: ValidationException =>
            log.log(Level.INFO, "Invalid transformation rule", ex)
            BadRequest(ex.toString)
          case ex: Exception =>
            log.log(Level.WARNING, "Failed to parse transformation rule", ex)
            InternalServerError("Error in back end: " + ex.getMessage)
        }
      case None =>
        BadRequest("Expecting text/xml request body")
    }
  }
  }

  def getRule(projectName: String, taskName: String, rule: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes

    task.data.rules.find(_.name == rule) match {
      case Some(r) => Ok(XmlSerialization.toXml(r))
      case None => NotFound(s"No rule named '$rule' found!")
    }
  }

  def putRule(projectName: String, taskName: String, ruleIndex: Int): Action[AnyContent] = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    implicit val readContext = ReadContext(resources, prefixes)

    request.body.asXml match {
      case Some(xml) =>
        try {
          //Collect warnings while parsing transformation rule
          val warnings = CollectLogs(Level.WARNING, "org.silkframework.linkagerule") {
            //Load transformation rule
            val updatedRule = XmlSerialization.fromXml[TransformRule](xml.head)
            val updatedRules = task.data.rules.updated(ruleIndex, updatedRule)
            val updatedTask = task.data.copy(rules = updatedRules)
            project.updateTask(taskName, updatedTask)
          }
          // Return warnings
          Ok(JsonError("Transform rule committed successfully", warnings.map(log => ValidationWarning(log.getMessage))))
        } catch {
          case ex: ValidationException =>
            log.log(Level.INFO, "Invalid transformation rule", ex)
            BadRequest(JsonError("Invalid transformation rule", ex.errors))
          case ex: Exception =>
            log.log(Level.WARNING, "Failed to commit transformation rule", ex)
            InternalServerError(JsonError("Failed to commit transformation rule", ValidationError("Error in back end: " + ex.getMessage) :: Nil))
        }
      case None =>
        BadRequest("Expecting text/xml request body")
    }
  }
  }

  def reloadTransformCache(projectName: String, taskName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    task.activity[TransformPathsCache].control.reset()
    task.activity[TransformPathsCache].control.start()
    Ok
  }

  def executeTransformTask(projectName: String, taskName: String): Action[AnyContent] = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val activity = task.activity[ExecuteTransform].control
    activity.start()
    Ok
  }

  /**
    * Given a search term, returns all possible completions for source property paths.
    */
  def sourcePathCompletions(projectName: String, taskName: String, term: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    var completions = Seq[String]()

    // Add known paths
    if (Option(task.activity[TransformPathsCache].value).isDefined) {
      val knownPaths = task.activity[TransformPathsCache].value.typedPaths
      // TODO: The paths could be typed, discuss
      completions ++= knownPaths.map(_.path.serializeSimplified(project.config.prefixes)).sorted
    }

    // Add known prefixes last
    val prefixCompletions = project.config.prefixes.prefixMap.keys.map(_ + ":")
    completions ++= prefixCompletions

    // Filter all completions that match the search term
    val matches = completions.filter(_.contains(term))

    // Convert to JSON and return
    Ok(JsArray(matches.map(JsString)))
  }

  /**
    * Given a search term, returns possible completions for target paths.
    *
    * @param projectName The name of the project
    * @param taskName    The name of the transformation
    * @param sourcePath  The source path to be completed. If none, types will be suggested
    * @param term        The search term
    * @return
    */
  def targetPathCompletions(projectName: String, taskName: String, sourcePath: Option[String], term: String): Action[AnyContent] = Action {
    val (project, task) = projectAndTask(projectName, taskName)
    val completions = TargetPathAutcompletion.retrieve(project, task, sourcePath, term)
    Ok(JsArray(completions.map(_.toJson)))
  }

  /**
    * Transform entities bundled with the request according to the transformation task.
    *
    * @param projectName
    * @param taskName
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

  private def executeTransform(task: ProjectTask[TransformSpec], entitySink: EntitySink, dataSource: DataSource, errorEntitySinkOpt: Option[EntitySink]): Unit = {
    val transform = new ExecuteTransform(dataSource,  task.data, Seq(entitySink))
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
            val rule = task.data.rules.find(_.name.toString == ruleName).getOrElse(
              throw new IllegalArgumentException(s"Transform task $taskName in project $projectName has no transformation rule $ruleName! Valid rule names: "
                  + task.data.rules.map(_.name).mkString(", "))
            )
            val entityDescription = oneRuleEntitySchema(transformTask, rule)
            val exampleEntities = peakDataSource.peak(entityDescription, maxTryEntities)
            generateMappingPreviewResponse(rule, exampleEntities, limit)
          case _ =>
            Ok(Json.toJson(PeakResults(None, None, PeakStatus("not supported", "Input dataset task " + inputTask.toString + " of type " + dataset.plugin.label +
                " does not support transformation preview!"))))
        }
      case _: ProjectTask[_] =>
        Ok(Json.toJson(PeakResults(None, None, PeakStatus("not supported", "Input task " + inputTask.toString +
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
          s"Transformation result was always empty or exceptions occurred. $tryCounter processed and $errorCounter exceptions occurred. First exception: " + errorMessage))))
    } else {
      Ok(Json.toJson(PeakResults(Some(rule.paths.map(serializePath)), Some(sourceAndTargetResults),
        status = PeakStatus("empty", s"Transformation result was always empty. Processed first $tryCounter entities."))))
    }
  }

  /**
    *
    * @param rule The transformation rule to execute on the example entities.
    * @param exampleEntities Entities to try executing the tranform rule on
    * @param limit Limit of examples to return
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