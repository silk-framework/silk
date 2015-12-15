package org.silkframework.workspace.activity.workflow

import java.util.logging.Logger

import org.silkframework.dataset.Dataset
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.workflow.Workflow.WorkflowOperator

class WorkflowExecutor(task: Task[Workflow]) extends Activity[Unit] {

  val log = Logger.getLogger(getClass.getName)

  override def run(context: ActivityContext[Unit]) = {
    val operators = task.data.operators

    // Preliminary: Just execute the operators from left to right
    for((op, index) <- operators.sortBy(_.position.x).zipWithIndex) {
      context.status.update(s"${op.task} (${index + 1} / ${operators.size})", index.toDouble / operators.size)
      executeOperator(op, context)
    }
  }

  def executeOperator(operator: WorkflowOperator, context: ActivityContext[Unit]) = {
    val project = task.project
    val inputs = operator.inputs.map(id => project.task[Dataset](id).data.source)
    val linkOutputs = operator.outputs.map{ id =>
      project.task[Dataset](id).data.linkSink
    }
    val entityOutputs = operator.outputs.map{ id =>
      project.task[Dataset](id).data.entitySink
    }
    val taskData = project.anyTask(operator.task).data

    val taskExecutor = project.getExecutor(taskData)
      .getOrElse(throw new Exception("Cannot execute task " + operator.task))

    val activity = taskExecutor(inputs, taskData, linkOutputs, entityOutputs)
    //TODO job.statusLogLevel = Level.FINE
    //TODO job.progressLogLevel = Level.FINE
    context.child(activity, 0.0).startBlocking()

    log.info("Finished execution of " + operator.task)
  }
}