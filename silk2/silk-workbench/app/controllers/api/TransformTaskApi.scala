package controllers.api

import controllers.tabs.TransformEditor._
import play.api.mvc.{Action, Controller}
import de.fuberlin.wiwiss.silk.workspace.User
import java.util.logging.{Logger, Level}
import play.api.libs.json.{JsArray, JsString, JsObject}
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.util.{ValidationException, CollectLogs}
import de.fuberlin.wiwiss.silk.util.ValidationException.ValidationError

object TransformTaskApi extends Controller {

  private val log = Logger.getLogger(getClass.getName)

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
          val updatedRules = (xml \ "TransformRule").map(TransformRule.load(project.resourceManager)(prefixes))
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
            val updatedRule = TransformRule.load(project.resourceManager)(prefixes)(xml.head)
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
            InternalServerError(statusJson(errors = ValidationError("Error in back end: " + ex.getMessage) :: Nil))
        }
      case None =>
        BadRequest("Expecting text/xml request body")
    }
  }}

  private def statusJson(errors: Seq[ValidationError] = Nil, warnings: Seq[String] = Nil, infos: Seq[String] = Nil) = {
    /**Generates a Json expression from an error */
    def errorToJsExp(error: ValidationError) = JsObject(("message", JsString(error.toString)) :: ("id", JsString(error.id.map(_.toString).getOrElse(""))) :: Nil)

    JsObject(
      ("error", JsArray(errors.map(errorToJsExp))) ::
          ("warning", JsArray(warnings.map(JsString(_)))) ::
          ("info", JsArray(infos.map(JsString(_)))) :: Nil
    )
  }

}
