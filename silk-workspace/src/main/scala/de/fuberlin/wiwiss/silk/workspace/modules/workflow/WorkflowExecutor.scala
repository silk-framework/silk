package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.runtime.activity.{Activity, ActivityContext}
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.Workflow.WorkflowOperator

class WorkflowExecutor(operators: Seq[WorkflowOperator], project: Project) extends Activity[Unit] {

  val log = Logger.getLogger(getClass.getName)

  override def run(context: ActivityContext[Unit]) = {
    val inputNames = operators.flatMap(_.inputs).toSet
    val outputNames = operators.flatMap(_.outputs).toSet

    // Determine all datasets that are filled by an operator
    var emptyDatasets = outputNames
    var pendingOperators = operators.toSet

    while (pendingOperators.nonEmpty) {
      // Execute next operator
      pendingOperators.find(!_.inputs.exists(emptyDatasets.contains)) match {
        case Some(op) =>
          // Update status
          val completedTasks = operators.size - pendingOperators.size
          context.status.update(s"${op.task} (${completedTasks + 1} / ${operators.size})", completedTasks.toDouble / operators.size)
          // Execute
          executeOperator(op, context)
          emptyDatasets --= op.outputs
          pendingOperators -= op
        case None =>
          throw new RuntimeException("Deadlock in workflow execution")
      }
    }
  }

  def executeOperator(operator: WorkflowOperator, context: ActivityContext[Unit]) = {
    val inputs = operator.inputs.map(id => project.task[Dataset](id).data.source)
    val outputs = operator.outputs.map(id => project.task[Dataset](id).data)
    val taskData = project.anyTask(operator.task).data

    val taskExecutor = project.getExecutor(taskData)
      .getOrElse(throw new Exception("Cannot execute task " + operator.task))

    val activity = taskExecutor(inputs, taskData, outputs)
    //TODO job.statusLogLevel = Level.FINE
    //TODO job.progressLogLevel = Level.FINE
    context.executeBlocking(activity, 1.0 / operators.size)

    log.info("Finished execution of " + operator.task)
  }
}