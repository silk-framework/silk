package controllers.workspaceApi.project

import controllers.errorReporting.ErrorReport.ErrorReportItem
import org.silkframework.config.MetaData
import org.silkframework.execution.report.Stacktrace
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext}
import org.silkframework.runtime.validation.{NotFoundException, RequestException}
import org.silkframework.workbench.utils.JsonRequestException
import org.silkframework.workspace.{Project, TaskLoadingError}
import play.api.libs.json.{JsObject, Json}

import java.net.HttpURLConnection
import scala.util.Try

/**
  * Used for the project task loading error report API.
  */
object ProjectLoadingErrors {

  def fromTaskLoadingError(taskLoadingError: TaskLoadingError): ErrorReportItem = {
    ErrorReportItem(
      projectId = taskLoadingError.projectId.map(_.toString),
      taskId = Some(taskLoadingError.taskId),
      activityId = None,
      errorSummary = "Loading failed: " + taskLoadingError.label.getOrElse(taskLoadingError.taskId),
      taskLabel = taskLoadingError.label,
      taskDescription = taskLoadingError.description,
      errorMessage = Option(taskLoadingError.throwable.getMessage),
      stackTrace = Some(Stacktrace.fromException(taskLoadingError.throwable))
    )
  }

  /**
    * Tries to reload all tasks that caused loading errors.
    * Loading errors that are resolved are removed from the project.
    */
  def tryReloadTasks(project: Project)(implicit user: UserContext): Unit = {
    for(loadingError <- project.loadingErrors) {
      Try(reloadTask(project, loadingError))
    }
  }

  /**
    * Reloads a task from a task loading error.
    *
    * @param project The project
    * @param taskLoadingError The task loading error
    * @param parameterValues Optionally overwrites parameter values.
    */
  def reloadTask(project: Project, taskLoadingError: TaskLoadingError, parameterValues: Option[ParameterValues] = None)
                (implicit user: UserContext): Unit = {
    taskLoadingError.factoryFunction match {
      case Some(reloadFunction) =>
        reloadFunction(parameterValues.getOrElse(ParameterValues.empty), PluginContext.fromProject(project)).taskOrError match {
          case Left(error) =>
            val taskLoadingError = ProjectLoadingErrors.fromTaskLoadingError(error)
            throw CannotReloadTaskException(taskLoadingError)
          case Right(task) =>
            project.updateAnyTask(task.id, task.data, Some(MetaData(label = taskLoadingError.label, description = taskLoadingError.description)))
            project.removeLoadingError(task.id)
        }
      case None =>
        throw new NotFoundException("The task cannot be reloaded.")
    }
  }

  /**
    * Thrown if a task could not be (re)loaded.
    */
  case class CannotReloadTaskException(taskLoadingError: ErrorReportItem)
    extends RequestException(s"The task could not be loaded. Summary: ${taskLoadingError.errorSummary}", None) with JsonRequestException {

    override def errorTitle: String = "Task loading error"

    override def httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)

    override def additionalJson: JsObject = {
      Json.obj(
        "taskLoadingError" -> Json.toJson(taskLoadingError)
      )
    }
  }
}

