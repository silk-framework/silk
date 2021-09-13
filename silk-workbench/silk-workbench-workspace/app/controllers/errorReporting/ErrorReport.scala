package controllers.errorReporting

import play.api.libs.json.{Format, Json}

object ErrorReport {
  /** Stacktrace object. */
  case class Stacktrace(errorMessage: Option[String], lines: Seq[String], cause: Option[Stacktrace])

  object Stacktrace {
    implicit val stacktraceJsonFormat: Format[Stacktrace] = Json.format[Stacktrace]

    def fromException(exception: Throwable): Stacktrace = {
      val lines = exception.getStackTrace.map(_.toString)
      val cause = Option(exception.getCause).map(fromException)
      Stacktrace(Option(exception.getMessage), lines, cause)
    }
  }

  /** A single item of an error report, i.e. documents an individual error/exception. */
  case class ErrorReportItem(projectId: Option[String],
                             taskId: Option[String],
                             activityId: Option[String],
                             errorSummary: String,
                             projectLabel: Option[String] = None,
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
      sb.append(s"\n\n* Error summary: $errorSummary\n")
      projectId.foreach(id => sb.append(s"* Project ID: $id\n"))
      projectLabel.foreach(id => sb.append(s"* Project label: $id\n"))
      taskId.foreach(id => sb.append(s"* Task ID: $id\n"))
      taskLabel.foreach(id => sb.append(s"* Task label: $id\n"))
      taskDescription foreach { taskDescription => sb.append(s"* Task description: $taskDescription\n")}
      activityId.foreach(id => sb.append(s"* Activity ID: $id\n"))
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

  object ErrorReportItem {
    implicit val projectTaskLoadingErrorJsonFormat: Format[ErrorReportItem] = Json.format[ErrorReportItem]
  }
}
