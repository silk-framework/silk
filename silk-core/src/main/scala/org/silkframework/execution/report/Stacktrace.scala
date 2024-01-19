package org.silkframework.execution.report

import scala.collection.immutable.ArraySeq

/** Stacktrace object with nested stack traces for causes. */
case class Stacktrace(exceptionClass: String,
                      errorMessage: Option[String],
                      lines: Seq[String],
                      cause: Option[Stacktrace],
                      suppressed: Seq[Stacktrace])

object Stacktrace {
  def fromException(exception: Throwable): Stacktrace = {
    val lines = ArraySeq.unsafeWrapArray(exception.getStackTrace.map(_.toString))
    val cause = Option(exception.getCause).map(fromException)
    val suppressed = ArraySeq.unsafeWrapArray(exception.getSuppressed).map(fromException)
    Stacktrace(exception.getClass.getName, Option(exception.getMessage), lines, cause, suppressed)
  }
}