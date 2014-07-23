package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowTask.WorkflowOperator

class WorkflowTask(val name: Identifier, val operators: Seq[WorkflowOperator]) extends ModuleTask {

  def execute() = {
    val executor = new WorkflowExecutor(operators)
    executor()
  }

}

object WorkflowTask {

  case class WorkflowOperator(inputs: Seq[String], task: ModuleTask, outputs: Seq[String])

}

class WorkflowExecutor(operators: Seq[WorkflowOperator]) {

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
  }

}