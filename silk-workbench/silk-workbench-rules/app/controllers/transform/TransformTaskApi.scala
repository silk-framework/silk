package controllers.transform

import java.util.logging.{Level, Logger}

import controllers.util.ProjectUtils._
import models.JsonError
import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.entity.{Path, Restriction}
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformRule, TransformSpec}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.runtime.validation.{ValidationError, ValidationException, ValidationWarning}
import org.silkframework.util.{CollectLogs, Identifier, Uri}
import org.silkframework.workspace.activity.transform.{MappingCandidates, TransformPathsCache}
import org.silkframework.workspace.{ProjectTask, User}
import play.api.libs.json.{JsArray, JsString}
import play.api.mvc.{Action, AnyContentAsXml, Controller}

class TransformTaskApi extends Controller {

  private val log = Logger.getLogger(getClass.getName)

  def putTransformTask(project: String, task: String) = Action { implicit request => {
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

  def deleteTransformTask(projectName: String, taskName: String, removeDependentTasks: Boolean) = Action {
    val project = User().workspace.project(projectName)
    if(removeDependentTasks) {
      for(dependentTransform <- project.tasks[TransformSpec].find(_.data.selection.inputId == taskName)) {
        project.removeTask[TransformSpec](dependentTransform.id)
      }
      for(dependentLinking <- project.tasks[LinkSpec].find(_.data.dataSelections.exists(_.inputId == taskName))) {
        project.removeTask[LinkSpec](dependentLinking.id)
      }
    }
    project.removeTask[TransformSpec](taskName)
    Ok
  }

  def getRules(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes

    Ok(<TransformRules>
      {task.data.rules.map(XmlSerialization.toXml[TransformRule])}
    </TransformRules>)
  }

  def putRules(projectName: String, taskName: String) = Action { request => {
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

  def getRule(projectName: String, taskName: String, rule: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    implicit val prefixes = project.config.prefixes

    task.data.rules.find(_.name == rule) match {
      case Some(r) => Ok(XmlSerialization.toXml(r))
      case None => NotFound(s"No rule named '$rule' found!")
    }
  }

  def putRule(projectName: String, taskName: String, ruleIndex: Int) = Action { request => {
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

  def reloadTransformCache(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    task.activity[TransformPathsCache].control.reset()
    task.activity[TransformPathsCache].control.start()
    Ok
  }

  def executeTransformTask(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val activity = task.activity[ExecuteTransform].control
    activity.start()
    Ok
  }

  /**
   * Given a search term, returns all possible completions for source property paths.
   */
  def sourcePathCompletions(projectName: String, taskName: String, term: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    var completions = Seq[String]()

    // Add known paths
    if (task.activity[TransformPathsCache].value != null) {
      val knownPaths = task.activity[TransformPathsCache].value.paths
      completions ++= knownPaths.map(_.serializeSimplified(project.config.prefixes)).sorted
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
    * @param taskName The name of the transformation
    * @param sourcePath The source path to be completed. If none, types will be suggested
    * @param term The search term
    * @return
    */
  def targetPathCompletions(projectName: String, taskName: String, sourcePath: Option[String], term: String) = Action {
    val (project, task) = projectAndTask(projectName, taskName)
    val prefixes = project.config.prefixes
    val maxCompletions = 20

    // Collect all caches with MappingCandidates and suggest completions
    val mappingCandidates = {
      for (activity <- task.activities if classOf[MappingCandidates].isAssignableFrom(activity.valueType)) yield {
        val candidates = activity.value.asInstanceOf[MappingCandidates]
        sourcePath match {
          case Some(path) => candidates.suggestProperties(Path.parse(path))
          case None => candidates.suggestTypes
        }
      }
    }

    val mappingCompletions = mappingCandidates.flatten.sortBy(-_.confidence).map(_.uri.toString)
    val prefixCompletions = prefixes.prefixMap.keys.toSeq.sorted.map(_ + ":")
    val completions = mappingCompletions ++ prefixCompletions

    // Filter all completions that match the search term
    var matches = completions.filter(_.contains(term)).take(maxCompletions)

    // If no completions match, return some suggestions anyway
    if(matches.isEmpty)
      matches = completions.take(maxCompletions)

    // Remove duplicates and shorten URIs
    matches = matches.distinct.map(prefixes.shorten)

    // Convert to JSON and return
    Ok(JsArray(matches.map(JsString)))
  }

  /**
   * Transform entities bundled with the request according to the transformation task.
    *
    * @param projectName
   * @param taskName
   * @return If no sink is specified in the request then return results in N-Triples format with the response,
   *         else write triples to defined data sink.
   */
  def postTransformInput(projectName: String, taskName: String) = Action { request =>
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
    val transform = new ExecuteTransform(dataSource, DatasetSelection.empty, task.data.rules, Seq(entitySink),errorEntitySinkOpt.toSeq)
    Activity(transform).startBlocking()
  }

  private def projectAndTask(projectName: String, taskName: String) = {
    getProjectAndTask[TransformSpec](projectName, taskName)
  }
}


