package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, Activity}
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowTask.WorkflowOperator

class WorkflowExecutor(operators: Seq[WorkflowOperator], project: Project) {

  val log = Logger.getLogger(getClass.getName)

  def apply(): Activity = {
    new ExecutorTask
  }

  class ExecutorTask extends Activity {

    override def run(context: ActivityContext) = {
      val inputNames = operators.flatMap(_.inputs).toSet
      val outputNames = operators.flatMap(_.outputs).toSet

      // Determine all datasets that are filled by an operator
      var emptyDatasets = outputNames
      var pendingOperators = operators.toSet

      while (pendingOperators.nonEmpty) {
        // Execute next operator
        pendingOperators.find(!_.inputs.exists(emptyDatasets.contains)) match {
          case Some(op) =>
            executeOperator(op, context)
            emptyDatasets --= op.outputs
            pendingOperators -= op
          case None =>
            throw new RuntimeException("Deadlock in workflow execution")
        }
        // Update status
        val completedTasks = operators.size - pendingOperators.size
        context.updateStatus(s"$completedTasks / ${operators.size}", completedTasks.toDouble / operators.size)
      }
    }

    def executeOperator(operator: WorkflowOperator, context: ActivityContext) = {
      log.info("Executing " + operator.task)

      val inputs = operator.inputs.map(id => project.task[DatasetTask](id).dataset.source)
      val outputs = operator.outputs.map(id => project.task[DatasetTask](id).dataset.sink)
      val task = project.anyTask(operator.task)

      val taskExecutor = project.getExecutor(task)
          .getOrElse(throw new Exception("Cannot execute task " + operator.task))

      val job = taskExecutor(inputs, task, outputs)
      //TODO job.statusLogLevel = Level.FINE
      //TODO job.progressLogLevel = Level.FINE
      context.executeBackground(job, 0.0)

      log.info("Finished execution of " + operator.task)
    }
  }

}