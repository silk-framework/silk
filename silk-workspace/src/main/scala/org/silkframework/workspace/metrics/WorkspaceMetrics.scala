package org.silkframework.workspace.metrics

import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.{Gauge, MeterRegistry}
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.{ProjectTask, Workspace}

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
    workspaceTaskSizesPerCategory(registry)
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

  private def workspaceTaskSizesPerCategory(registry: MeterRegistry): Unit = {
    Try {
      val allTasks: Seq[ProjectTask[_ <: TaskSpec]] = workspace.projects.flatMap(_.allTasks)

      val allTasksBySpec: Map[Class[ProjectTask[_ <: TaskSpec]], Seq[ProjectTask[_ <: TaskSpec]]] =
        allTasks.groupBy(_.data.getClass)

      allTasksBySpec.foreach { specAndTasks =>
        val clazz: Class[ProjectTask[_ <: TaskSpec]] = specAndTasks._1
        val tasks: Seq[ProjectTask[_ <: TaskSpec]] = specAndTasks._2

        Gauge.builder("task.size", () => tasks.size)
          .description("Workspace task size, per task specification")
          .tags("spec", clazz.getSimpleName)
          .register(registry)
      }
    }
  }
}
