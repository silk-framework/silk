package controllers.transform

import java.util.logging.{Level, Logger}

import controllers.util.ProjectUtils._
import controllers.util.SerializationUtils._
import org.silkframework.config.{Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.rule._
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException, ValidationError, ValidationException}
import org.silkframework.serialization.json.JsonParseException
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.util.{Identifier, IdentifierGenerator, Uri}
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workspace.activity.transform.TransformPathsCache
import org.silkframework.workspace.{ProjectTask, User}
import play.api.libs.json._
import play.api.mvc._

class TransformTaskApi extends Controller {

  private val log = Logger.getLogger(getClass.getName)

  def getTransformTask(projectName: String, taskName: String): Action[AnyContent] = Action { implicit request =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    serializeCompileTime[Task[TransformSpec]](task)
  }

  def putTransformTask(projectName: String, taskName: String): Action[AnyContent] = Action { implicit request => {
    val project = getProject(projectName)
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val readContext: ReadContext = ReadContext()

    request.body match {
      case AnyContentAsFormUrlEncoded(v) =>
        val values = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)
        val input = DatasetSelection(values("source"), Uri.parse(values.getOrElse("sourceType", ""), prefixes),
          Restriction.custom(values.getOrElse("restriction", "")))
        val outputs = values.get("output").filter(_.nonEmpty).map(Identifier(_)).toSeq
        val targetVocabularies = values.get("targetVocabularies").toSeq.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)

        project.tasks[TransformSpec].find(_.id == taskName) match {
          //Update existing task
          case Some(oldTask) =>
            val updatedTransformSpec = oldTask.data.copy(selection = input, outputs = outputs, targetVocabularies = targetVocabularies)
            project.updateTask(taskName, updatedTransformSpec)
          //Create new task with no rule
          case None =>
            val transformSpec = TransformSpec(input, RootMappingRule("root", MappingRules.empty), outputs, Seq.empty, targetVocabularies)
            project.updateTask(taskName, transformSpec)
        }

        Ok
      case _ =>
        catchExceptions {
          deserializeCompileTime[Task[TransformSpec]]() { task =>
            project.updateTask(task.id, task.data)
            Ok
          }
        }
    }
  }
  }

  def deleteTransformTask(projectName: String, taskName: String, removeDependentTasks: Boolean): Action[AnyContent] = Action {
    val project = getProject(projectName)
    project.removeAnyTask(taskName, removeDependentTasks)

    Ok
  }

  def getRules(projectName: String, taskName: String): Action[AnyContent] = Action { implicit request =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    serializeCompileTime(task.data.mappingRule)
  }

  def putRules(projectName: String, taskName: String): Action[AnyContent] = Action { implicit request =>
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

  def getRule(projectName: String, taskName: String, ruleId: String): Action[AnyContent] = Action { implicit request =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    processRule(task, ruleId) { rule =>
      serializeCompileTime(rule.operator.asInstanceOf[TransformRule])
    }
  }

  def putRule(projectName: String, taskName: String, ruleId: String): Action[AnyContent] = Action { request =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val resources: ResourceManager = project.resources
    implicit val readContext: ReadContext = ReadContext(resources, prefixes, identifierGenerator(task))

    task.synchronized {
      processRule(task, ruleId) { currentRule =>
        implicit val updatedRequest: Request[AnyContent] = updateJsonRequest(request, currentRule)
        deserializeCompileTime[TransformRule]() { updatedRule =>
          updateRule(currentRule.update(updatedRule))
          serializeCompileTime[TransformRule](updatedRule)
        }
      }
    }
  }

  def deleteRule(projectName: String, taskName: String, rule: String): Action[AnyContent] = Action { implicit request =>
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

  def appendRule(projectName: String, taskName: String, ruleName: String): Action[AnyContent] = Action { implicit request =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes
    task.synchronized {
      implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes, identifierGenerator(task))
      processRule(task, ruleName) { parentRule =>
        deserializeCompileTime[TransformRule]() { newChildRule =>
          if(task.data.nestedRuleAndSourcePath(newChildRule.id).isDefined) {
            throw new ValidationException(s"Rule with ID ${newChildRule.id} already exists!")
          }
          val updatedRule = parentRule.operator.withChildren(parentRule.operator.children :+ newChildRule)
          updateRule(parentRule.update(updatedRule))
          serializeCompileTime(newChildRule)
        }
      }
    }
  }

  def reorderRules(projectName: String, taskName: String, ruleName: String): Action[AnyContent] = Action { implicit request =>
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
    * Transform entities bundled with the request according to the transformation task.
    *
    * @return If no sink is specified in the request then return results in N-Triples format with the response,
    *         else write triples to defined data sink.
    */
  def postTransformInput(projectName: String, taskName: String): Action[AnyContent] = Action { request =>
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
                               errorEntitySinkOpt: Option[EntitySink]): Unit = {
    val transform = new ExecuteTransform(dataSource, task.data, entitySink)
    Activity(transform).startBlocking()
  }

  private def projectAndTask(projectName: String, taskName: String) = {
    getProjectAndTask[TransformSpec](projectName, taskName)
  }

  def valueSourcePaths(projectName: String,
                       taskName: String,
                       ruleId: String,
                       maxDepth: Int,
                       unusedOnly: Boolean): Action[AnyContent] = Action { implicit request =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    task.nestedRuleAndSourcePath(ruleId) match {
      case Some((_, sourcePath)) =>
        val pathCache = task.activity[TransformPathsCache]
        pathCache.control.waitUntilFinished()
        val cachedPaths = pathCache.value.fetchCachedPaths(task, sourcePath)
        val isRdfInput = pathCache.value.isRdfInput(task)
        val matchingPaths = cachedPaths filter { p =>
          val pathSize = p.path.operators.size
          isRdfInput ||
              p.path.operators.startsWith(sourcePath) &&
                  pathSize > sourcePath.size &&
                  pathSize - sourcePath.size <= maxDepth
        } map { p =>
            if(isRdfInput) {
              p.path
            } else {
              Path(p.path.operators.drop(sourcePath.size))
            }
        }
        val filteredPaths = if(unusedOnly) {
          val sourcePaths = task.data.valueSourcePaths(ruleId, maxDepth)
          matchingPaths.filterNot { path =>
            sourcePaths.contains(path)
          }
        } else {
          matchingPaths
        }
        Ok(Json.toJson(filteredPaths.map(_.serialize)))
      case None =>
        NotFound("No rule found with ID " + ruleId)
    }
  }
}

// Peak API
case class PeakResults(sourcePaths: Option[Seq[Seq[String]]], results: Option[Seq[PeakResult]], status: PeakStatus)

case class PeakStatus(id: String, msg: String)

case class PeakResult(sourceValues: Seq[Seq[String]], transformedValues: Seq[String])