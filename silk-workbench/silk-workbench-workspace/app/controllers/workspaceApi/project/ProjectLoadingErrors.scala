package controllers.workspaceApi.project

import org.silkframework.workspace.TaskLoadingError
import play.api.libs.json.{Format, Json}

/**
  * Used for the project task loading error report API.
  */
object ProjectLoadingErrors {
  /** Task loading failure report. */
  case class ProjectTaskLoadingErrorResponse(taskId: String,
                                             errorSummary: String,
                                             taskLabel: Option[String] = None,
                                             taskDescription: Option[String] = None,
                                             errorMessage: Option[String] = None,
                                             stackTrace: Option[Stacktrace] = None) {
    private def readableTaskName = taskLabel.getOrElse(taskId)
    /** Render as markdown.
      *
      * @param taskNr If rendered in an overall task report, this is the index of the failed task.
      */
    def asMarkdown(taskNr: Option[Int]): String = {
      val sb = new StringBuffer()
      taskNr match {
        case Some(nr) => sb.append(s"## Task $nr: $readableTaskName")
        case None => sb.append("## Details")
      }
      sb.append(s"\n\n* Task ID: $taskId\n")
      sb.append(s"* Error summary: $errorSummary\n")
      taskLabel foreach { label => sb.append(s"* Task label: $label\n")}
      taskDescription foreach { taskDescription => sb.append(s"* Task description: $taskDescription\n")}
      errorMessage foreach { message => sb.append(s"* Error message: `$message`\n")}
      stackTrace foreach { st => sb.append(asMarkdown(st))}
      sb.toString
    }

    // Render the stacktrace as markdown
    private def asMarkdown(stacktrace: Stacktrace): String = {
      val sb = new StringBuffer()
      def renderStacktrace(stacktrace: Stacktrace): Unit = {
        for(line <- stacktrace.lines) {
          sb.append("  ").append(line).append("\n")
        }
        stacktrace.cause foreach { c =>
          sb.append(s"Cause: ${stacktrace.errorMessage.getOrElse("<no error message>")}\n")
          renderStacktrace(c)}
      }
      sb.append("* Stacktrace:\n  ```\n")
      renderStacktrace(stacktrace)
      sb.append("  ```\n")
      sb.toString
    }
  }

  case class Stacktrace(errorMessage: Option[String], lines: Seq[String], cause: Option[Stacktrace])

  object Stacktrace {
    def fromException(exception: Throwable): Stacktrace = {
      val lines = exception.getStackTrace.map(_.toString)
      val cause = Option(exception.getCause).map(fromException)
      Stacktrace(Option(exception.getMessage), lines, cause)
    }
  }

  object ProjectTaskLoadingErrorResponse {
    implicit val stacktraceJsonFormat: Format[Stacktrace] = Json.format[Stacktrace]
    implicit val projectTaskLoadingErrorJsonFormat: Format[ProjectTaskLoadingErrorResponse] = Json.format[ProjectTaskLoadingErrorResponse]

    def fromTaskLoadingError(taskLoadingError: TaskLoadingError): ProjectTaskLoadingErrorResponse = {
      ProjectTaskLoadingErrorResponse(
        taskId = taskLoadingError.id,
        errorSummary = "Loading failed: " + taskLoadingError.label.getOrElse(taskLoadingError.id),
        taskLabel = taskLoadingError.label,
        taskDescription = taskLoadingError.description,
        errorMessage = Option(taskLoadingError.throwable.getMessage),
        stackTrace = Some(Stacktrace.fromException(taskLoadingError.throwable))
      )
    }
  }
}