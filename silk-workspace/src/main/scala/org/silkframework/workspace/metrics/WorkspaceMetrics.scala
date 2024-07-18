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
    Gauge.builder(
        "workspace.metrics.project.size",
        workspace,
        (w: Workspace) => Try(w.projects.size.toDouble).getOrElse(0.0)
      )
      .description("Workspace metrics: project size")
      .register(registry)
  }
}
