package controllers.workspaceApi.project

import controllers.errorReporting.ErrorReport.{ErrorReportItem, Stacktrace}
import org.silkframework.workspace.TaskLoadingError
import play.api.libs.json.{Format, Json}

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
}