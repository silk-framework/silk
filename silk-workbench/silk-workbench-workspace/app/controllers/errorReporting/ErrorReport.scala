package controllers.errorReporting

import org.silkframework.workbench.utils.ErrorResult.Stacktrace
import play.api.libs.json.{Format, Json}
import org.silkframework.serialization.json.ExecutionReportSerializers._

object ErrorReport {
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
    private def readableTaskName = taskLabel.getOrElse(taskId.getOrElse("-"))
    /** Render as markdown.
      *
      * @param taskNr If rendered in an overall task report, this is the index of the failed task.
      */
    def asMarkdown(taskNr: Option[Int]): String = {
      val sb = new StringBuilder()
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
      for((subErrorMessages, idx) <- stackTrace.map(subErrorMessages).zipWithIndex;
          subErrorMessage <- subErrorMessages) {
        if(idx == 0) {
          sb.append(s"* Cause error messages:\n")
        }
        sb.append(s"  - $subErrorMessage\n")
      }
      stackTrace foreach { st => sb.append(asMarkdown(st))}
      sb.toString
    }

    private def subErrorMessages(stackTrace: Stacktrace): List[String] = {
      stackTrace.cause match {
        case Some(c) =>
          c.errorMessage match {
            case Some(msg) =>
              msg :: subErrorMessages(c)
            case None =>
              subErrorMessages(c)
          }
        case None =>
          Nil
      }
    }

    private def intend(str: String, width: Int): String = (" " * width) + str.dropWhile(_.isWhitespace).replaceAll("\n", "\n" + (" " * width))

    // Render the stacktrace as markdown
    private def asMarkdown(stacktrace: Stacktrace): String = {
      val sb = new StringBuilder()
      def renderStacktrace(stacktrace: Stacktrace): Unit = {
        for(line <- stacktrace.lines) {
          sb.append(intend("at " + line, 4)).append("\n")
        }
        stacktrace.suppressed foreach { c =>
          sb.append(intend(s"Suppressed: ${c.exceptionClass}: ${c.errorMessage.getOrElse("<no error message>")}", 2)).append("\n")
          renderStacktrace(c)
        }
        stacktrace.cause foreach { c =>
          sb.append(intend(s"Cause: ${c.exceptionClass}: ${c.errorMessage.getOrElse("<no error message>")}", 2)).append("\n")
          renderStacktrace(c)
        }
      }
      sb.append("* Stacktrace:\n  ```\n")
      sb.append(intend(s"${stacktrace.exceptionClass}: ${stacktrace.errorMessage.getOrElse("<no error message>")}", 2))
      renderStacktrace(stacktrace)
      sb.append("  ```\n")
      sb.toString
    }
  }

  object ErrorReportItem {
    implicit val projectTaskLoadingErrorJsonFormat: Format[ErrorReportItem] = Json.format[ErrorReportItem]
  }
}
