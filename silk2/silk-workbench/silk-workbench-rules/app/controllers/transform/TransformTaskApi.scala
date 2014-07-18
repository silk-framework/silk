package controllers.transform

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.config.Dataset
import de.fuberlin.wiwiss.silk.entity.{ForwardOperator, SparqlRestriction}
import de.fuberlin.wiwiss.silk.execution.ExecuteTransform
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.util.{CollectLogs, ValidationException}
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask
import de.fuberlin.wiwiss.silk.workspace.{Constants, User}
import models.transform.CurrentExecuteTransformTask
import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.mvc.{Action, Controller}

object TransformTaskApi extends Controller {

  private val log = Logger.getLogger(getClass.getName)

  def putTransformTask(project: String, task: String) = Action { implicit request => {
    val values = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)

    val proj = User().workspace.project(project)
    implicit val prefixes = proj.config.prefixes

    val dataset = Dataset(values("source"), Constants.SourceVariable, SparqlRestriction.fromSparql(Constants.SourceVariable, values("restriction")))

    proj.transformModule.tasks.find(_.name == task) match {
      //Update existing task
      case Some(oldTask) => {
        val updatedTransformTask = oldTask.updateDataset(dataset, proj)
        proj.transformModule.update(updatedTransformTask)
      }
      //Create new task with a single rule
      case None => {
        val transformTask = TransformTask(proj, task, dataset, Seq.empty)
        proj.transformModule.update(transformTask)
      }
    }
    Ok
  }}

  def deleteTransformTask(project: String, task: String) = Action {
    User().workspace.project(project).transformModule.remove(task)
    Ok
  }

  def getRules(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)
    implicit val prefixes = project.config.prefixes

    Ok(<TransformRules>{ task.rules.map(_.toXML) }</TransformRules>)
  }

  def putRules(projectName: String, taskName: String) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)
    implicit val prefixes = project.config.prefixes

    request.body.asXml match {
      case Some(xml) =>
        try {
          //Parse transformation rules
          val updatedRules = (xml \ "TransformRule").map(TransformRule.load(project.resources)(prefixes))
          //Update transformation task
          val updatedTask = task.updateRules(updatedRules, project)
          project.transformModule.update(updatedTask)
          Ok
        } catch {
          case ex: ValidationException =>
            BadRequest(ex.toString)
          case ex: Exception =>
            InternalServerError("Error in back end: " + ex.getMessage)
        }
      case None =>
        BadRequest("Expecting text/xml request body")
    }
  }}

  def getRule(projectName: String, taskName: String, rule: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)
    implicit val prefixes = project.config.prefixes

    task.rules.find(_.name == rule) match {
      case Some(r) => Ok(r.toXML)
      case None => NotFound(s"No rule named '$rule' found!")
    }
  }

  def putRule(projectName: String, taskName: String, ruleIndex: Int) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)
    implicit val prefixes = project.config.prefixes

    request.body.asXml match {
      case Some(xml) =>
        try {
          //Collect warnings while parsing transformation rule
          val warnings = CollectLogs(Level.WARNING, "de.fuberlin.wiwiss.silk.linkagerule") {
            //Load transformation rule
            val updatedRule = TransformRule.load(project.resources)(prefixes)(xml.head)
            val updatedRules = task.rules.updated(ruleIndex, updatedRule)
            //Update transformation task
            val updatedTask = task.updateRules(updatedRules, project)
            project.transformModule.update(updatedTask)
          }
          // Return warnings
          Ok(statusJson(warnings = warnings.map(_.getMessage)))
        } catch {
          case ex: ValidationException =>
            log.log(Level.INFO, "Invalid transformation rule")
            BadRequest(statusJson(errors = ex.errors))
          case ex: Exception =>
            log.log(Level.INFO, "Failed to save transformation rule", ex)
            InternalServerError(statusJson(errors =ValidationException.ValidationError("Error in back end: " + ex.getMessage) :: Nil))
        }
      case None =>
        BadRequest("Expecting text/xml request body")
    }
  }}

  private def statusJson(errors: Seq[ValidationException.ValidationError] = Nil, warnings: Seq[String] = Nil, infos: Seq[String] = Nil) = {
    /**Generates a Json expression from an error */
    def errorToJsExp(error: ValidationException.ValidationError) = JsObject(("message", JsString(error.toString)) :: ("id", JsString(error.id.map(_.toString).getOrElse(""))) :: Nil)

    JsObject(
      ("error", JsArray(errors.map(errorToJsExp))) ::
          ("warning", JsArray(warnings.map(JsString))) ::
          ("info", JsArray(infos.map(JsString))) :: Nil
    )
  }

  def reloadTransformCache(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)
    task.cache.clear()
    task.cache.load(project, task)
    Ok
  }

  def executeTransformTask(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)

    // Retrieve parameters
    val params = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    val outputNames = params.get("outputs[]").toSeq.flatten
    val outputs = outputNames.map(project.outputModule.task(_).output)

    // Create execution task
    val executeTransformTask =
      new ExecuteTransform(
        source = project.sourceModule.task(task.dataset.sourceId).source,
        dataset= task.dataset,
        rules = task.rules,
        outputs = outputs
      )

    // Start task in the background
    CurrentExecuteTransformTask() = executeTransformTask
    executeTransformTask.runInBackground()

    Ok
  }

  /**
   * Given a search term, returns all possible completions for source property paths.
   */
  def sourcePathCompletions(projectName: String, taskName: String, term: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)
    val knownPaths = task.cache.value.paths

    // Add known paths, use short notation for paths that only consist of a single property
    val pathCompletions =
      for(path <- knownPaths) yield {
        path.operators match {
          case ForwardOperator(p) :: Nil => p.toTurtle(project.config.prefixes)
          case _ => path.serialize(project.config.prefixes)
        }
      }

    // Add known prefixes last
    val allCompletions = pathCompletions ++  project.config.prefixes.prefixMap.keys.map(_ + ":")

    // Filter all completions that match the search term
    val matches = allCompletions.filter(_.contains(term)).take(20)

    // Convert to JSON and return
    Ok(JsArray(matches.map(JsString)))
  }

  def targetPathCompletions(projectName: String, taskName: String, term: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)

    // Collect known prefixes
    val prefixCompletions = project.config.prefixes.prefixMap.keys

    // Filter all completions that match the search term
    val matches = prefixCompletions.filter(_.contains(term)).toSeq.sorted.take(20).map(_ + ":")

    // Convert to JSON and return
    Ok(JsArray(matches.map(JsString)))
  }
}
