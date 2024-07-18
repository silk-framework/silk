package org.silkframework.workspace.metrics

import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.{Gauge, MeterRegistry}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.Workspace

import scala.util.Try

/**
 * Metrics for a Workspace instance.
 *
 * @param workspace Workspace to monitor.
 * @param userContext User context needed to retrieve the projects, etc.
 */
class WorkspaceMetrics(workspace: Workspace)(implicit userContext: UserContext) extends MeterBinder {
  override def bindTo(registry: MeterRegistry): Unit = {
    workspaceProjectSize(registry)
    workspaceTaskSize(registry)
  }

  private def workspaceProjectSize(registry: MeterRegistry): Unit = {
    Gauge.builder(
        "workspace.project.size",
        workspace,
        (w: Workspace) => Try(w.projects.size.toDouble).getOrElse(0.0)
      )
      .description("Workspace project size")
      .register(registry)
  }

  private def workspaceTaskSize(registry: MeterRegistry): Unit = {
    Gauge.builder(
        "workspace.task.size",
        workspace,
        (w: Workspace) => Try(w.projects.flatMap(_.allTasks).size.toDouble).getOrElse(0.0)
      )
      .description("Workspace task size")
      .register(registry)
  }
}
