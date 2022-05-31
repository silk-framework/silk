package controllers.workspaceApi.coreApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import play.api.mvc._

import java.lang.management.{LockInfo, ManagementFactory, ThreadInfo}
import javax.inject.Inject
import scala.collection.mutable

@Tag(name = "Workbench")
class SystemApi @Inject()() extends InjectedController {

  @Operation(
    summary = "Request thread dump",
    description = "Requests a thread dump for all live threads.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "text/plain"
        ))
      )
    ))
  def threadDump(): Action[AnyContent] = Action { implicit request => {
    val threadDump = new mutable.StringBuilder()
    val threadMXBean = ManagementFactory.getThreadMXBean
    for (threadInfo <- threadMXBean.dumpAllThreads(true, true)) {
      threadInfoToString(threadInfo, threadDump)
    }
    Ok(threadDump.toString)
  }}

  /**
    * Prints a thread info.
    * Based on ThreadInfo.toString(), but does print all frames instead of the first 8.
    */
  private def threadInfoToString(threadInfo: ThreadInfo, sb: mutable.StringBuilder) = {
    sb.append("\"" + threadInfo.getThreadName + "\"" + (if (threadInfo.isDaemon) " daemon"
    else "") + " prio=" + threadInfo.getPriority + " Id=" + threadInfo.getThreadId + " " + threadInfo.getThreadState)
    if (threadInfo.getLockName != null) sb.append(" on " + threadInfo.getLockName)
    if (threadInfo.getLockOwnerName != null) sb.append(" owned by \"" + threadInfo.getLockOwnerName + "\" Id=" + threadInfo.getLockOwnerId)
    if (threadInfo.isSuspended) sb.append(" (suspended)")
    if (threadInfo.isInNative) sb.append(" (in native)")
    sb.append('\n')

    val stackTrace = threadInfo.getStackTrace
    var i: Int = 0
    while (i < stackTrace.length) {
      val ste: StackTraceElement = stackTrace(i)
      sb.append("\tat " + ste.toString)
      sb.append('\n')
      if (i == 0 && threadInfo.getLockInfo != null) {
        val ts: Thread.State = threadInfo.getThreadState
        ts match {
          case Thread.State.BLOCKED =>
            sb.append("\t-  blocked on " + threadInfo.getLockInfo)
            sb.append('\n')

          case Thread.State.WAITING =>
            sb.append("\t-  waiting on " + threadInfo.getLockInfo)
            sb.append('\n')

          case Thread.State.TIMED_WAITING =>
            sb.append("\t-  waiting on " + threadInfo.getLockInfo)
            sb.append('\n')

          case _ =>
        }
      }
      for (mi <- threadInfo.getLockedMonitors) {
        if (mi.getLockedStackDepth == i) {
          sb.append("\t-  locked " + mi)
          sb.append('\n')
        }
      }

      i += 1
    }
    val locks: Array[LockInfo] = threadInfo.getLockedSynchronizers
    if (locks.length > 0) {
      sb.append("\n\tNumber of locked synchronizers = " + locks.length)
      sb.append('\n')
      for (li <- locks) {
        sb.append("\t- " + li)
        sb.append('\n')
      }
    }
    sb.append('\n')
    sb.toString
  }
}

