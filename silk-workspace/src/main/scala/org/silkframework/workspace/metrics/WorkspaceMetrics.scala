package org.silkframework.workspace.metrics

import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.{Gauge, MeterRegistry}
import org.silkframework.config.TaskSpec
import org.silkframework.workspace.{Project, ProjectTask}

import scala.util.Try

/**
 * Metrics for a Workspace instance.
 */
class WorkspaceMetrics(projectProvider: () => Seq[Project],
                       tasksProvider: () => Seq[ProjectTask[_ <: TaskSpec]]) extends MeterBinder {
  override def bindTo(registry: MeterRegistry): Unit = {
    workspaceProjectSize(registry)
    workspaceTaskSize(registry)
    workspaceTaskSizesPerCategory(registry)
  }

  private def workspaceProjectSize(registry: MeterRegistry): Unit = {
    Gauge.builder("workspace.project.size", () => Try(projectProvider().size).getOrElse(0).toDouble)
      .description("Workspace project size")
      .register(registry)
  }

  private def workspaceTaskSize(registry: MeterRegistry): Unit = {
    Gauge.builder("workspace.task.size", () => Try(tasksProvider().size).getOrElse(0).toDouble)
      .description("Workspace task size")
      .register(registry)
  }

  private def workspaceTaskSizesPerCategory(registry: MeterRegistry): Unit = {
    Try {
      val allTasks: Seq[ProjectTask[_ <: TaskSpec]] = tasksProvider()
      val allTasksBySpec: Map[Class[_], Seq[ProjectTask[_ <: TaskSpec]]] = allTasks.groupBy(_.taskType)

      allTasksBySpec.foreach { specAndTasks =>
        val clazz: Class[_] = specAndTasks._1
        val tasks: Seq[ProjectTask[_ <: TaskSpec]] = specAndTasks._2

        Gauge.builder("task.size", () => tasks.size)
          .description("Workspace task size, per task specification")
          .tags("spec", clazz.getSimpleName)
          .register(registry)
      }
    }
  }
}
