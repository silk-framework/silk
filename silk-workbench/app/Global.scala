import org.silkframework.runtime.activity.Activity
import org.silkframework.workbench.rules.{LinkingPlugin, TransformPlugin}
import org.silkframework.workbench.workflow.WorkflowPlugin
import play.api.Application
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.mvc.EssentialAction

import scala.concurrent.ExecutionContext.Implicits.global

object Global extends WorkbenchGlobal {

  override def beforeStart(app: Application) {
    super.beforeStart(app)
  }

  // Add HTTP caching headers to responses
  override def doFilter(action: EssentialAction): EssentialAction = EssentialAction { request =>
    action(request).map(result =>
      result.header.headers.get(CACHE_CONTROL) match {
        case None =>
          // Only set caching header if not already set
          result.withHeaders(
            CACHE_CONTROL -> "no-cache, no-store, max-age=0, must-revalidate"
          )
        case _ =>
          result
      }
    )
  }
}
