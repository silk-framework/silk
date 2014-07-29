package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowTask.WorkflowOperator

class WorkflowExecutor(operators: Seq[WorkflowOperator], project: Project) {

  val log = Logger.getLogger(getClass.getName)

  def apply() = {

    val inputNames = operators.flatMap(_.inputs).toSet
    val outputNames = operators.flatMap(_.outputs).toSet

    // Determine all datasets that are filled by an operator
    var emptyDatasets = outputNames
    var pendingOperators = operators.toSet

    while (pendingOperators.nonEmpty) {
      pendingOperators.find(!_.inputs.exists(emptyDatasets.contains)) match {
        case Some(op) =>
          execute(op)
          emptyDatasets --= op.outputs
          pendingOperators -= op
        case None =>
          throw new RuntimeException("Deadlock in workflow execution")
      }
    }
  }

  def execute(operator: WorkflowOperator) = {
    log.info("Executing " + operator.task.name)

    val inputs = operator.inputs.map(id => project.task[DatasetTask](id).dataset)
    val outputs = operator.outputs.map(id => project.task[DatasetTask](id).dataset)

    val taskExecutor = project.getExecutor(operator.task)
        .getOrElse(throw new Exception("Cannot execute task " + operator.task.name))

    taskExecutor(inputs, operator.task, outputs).apply()

    log.info("Finished execution of " + operator.task.name)
  }

}