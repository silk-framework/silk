package org.silkframework.workspace.metrics

import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.{Gauge, MeterRegistry}
import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{Project, ProjectTask}

import scala.util.Try

/**
 * Metrics for a Workspace instance.
 */
class WorkspaceMetrics(prefix: String,
                       projectProvider: () => Seq[Project],
                       tasksProvider: () => Seq[ProjectTask[_ <: TaskSpec]])
                      (implicit userContext: UserContext)
  extends MeterBinder {
  override def bindTo(registry: MeterRegistry): Unit = {
    workspaceProjectSize(registry)
    workspaceTaskSize(registry)
    workspaceTaskSizesPerCategory(registry)
  }

  private def workspaceProjectSize(registry: MeterRegistry): Unit = {
    Gauge.builder(s"$prefix.workspace.project.count", () => Try(projectProvider().size).getOrElse(0).toDouble)
      .description("Workspace project count")
      .register(registry)
  }

  private def workspaceTaskSize(registry: MeterRegistry): Unit = {
    Gauge.builder(s"$prefix.workspace.task.count", () => Try(tasksProvider().size).getOrElse(0).toDouble)
      .description("Workspace task count")
      .register(registry)
  }

  private def workspaceTaskSizesPerCategory(registry: MeterRegistry): Unit = Try {
    def projects: Seq[Project] = projectProvider()

    def transformTasks: Seq[ProjectTask[TransformSpec]] = projects.flatMap(_.tasks[TransformSpec])
    def datasetTasks: Seq[ProjectTask[DatasetSpec[_]]] = projects.flatMap(_.tasks[DatasetSpec[_]])
    def linkTasks: Seq[ProjectTask[LinkSpec]] = projects.flatMap(_.tasks[LinkSpec])
    def customTasks: Seq[ProjectTask[CustomTask]] = projects.flatMap(_.tasks[CustomTask])
    def workflowTasks: Seq[ProjectTask[Workflow]] = projects.flatMap(_.tasks[Workflow])

    def gauge[TaskType <: TaskSpec](taskProvider: () => Seq[ProjectTask[TaskType]], taskType: String): Unit =
      Gauge.builder(s"$prefix.workspace.task.count", () => taskProvider().size)
        .description("Workspace task count, per task type")
        .tags("type", taskType)
        .register(registry)

    gauge(() => transformTasks, "Transform")
    gauge(() => datasetTasks, "Dataset")
    gauge(() => linkTasks, "Linking")
    gauge(() => customTasks, "Task")
    gauge(() => workflowTasks, "Workflow")
  }
}
