package controllers.workspaceApi.coreApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import play.api.mvc._

import java.lang.management.ManagementFactory
import javax.inject.Inject

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
    val threadDump = new StringBuilder()
    val threadMXBean = ManagementFactory.getThreadMXBean
    for (threadInfo <- threadMXBean.dumpAllThreads(true, true)) {
      threadDump.append(threadInfo.toString)
    }
    Ok(threadDump.toString)
  }}
}

