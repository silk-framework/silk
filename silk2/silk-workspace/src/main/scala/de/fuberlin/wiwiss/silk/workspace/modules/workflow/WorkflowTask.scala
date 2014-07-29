package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowTask.{WorkflowDataset, WorkflowOperator}

import scala.xml.Node

class WorkflowTask(val name: Identifier,
                   val operators: Seq[WorkflowOperator],
                   val datasets: Seq[WorkflowDataset]) extends ModuleTask {

  def execute() = {
    val executor = new WorkflowExecutor(operators)
    executor()
  }

  def toXML = {
    <Workflow>{
      for(op <- operators) yield {
        <Operator
          posX={op.position._1.toString}
          posY={op.position._2.toString}
          task={op.task.name.toString}
          inputs={op.inputs.mkString(",")}
          outputs={op.outputs.mkString(",")} />
      }
    }{
      for(ds <- datasets) yield {
        <Dataset
          posX={ds.position._1.toString}
          posY={ds.position._2.toString}
          task={ds.task.name.toString} />
      }
    }</Workflow>
  }

}

object WorkflowTask {

  def fromXML(name: Identifier, xml: Node, project: Project) = {
    val operators =
      for(op <- xml \ "Operator") yield {
        WorkflowOperator(
          inputs = (op \ "@inputs").text.split(',').toSeq,
          task = project.anyTask((op \ "@task").text),
          outputs = (op \ "@outputs").text.split(',').toSeq,
          position = ((op \ "@posX").text.toInt, (op \ "@posY").text.toInt)
        )
      }

    val datasets =
      for(ds <- xml \ "Dataset") yield {
        WorkflowDataset(
          task = project.task[DatasetTask]((ds \ "@task").text),
          position = ((ds \ "@posX").text.toInt, (ds \ "@posY").text.toInt)
        )
      }

    new WorkflowTask(name, operators, datasets)
  }

  case class WorkflowOperator(inputs: Seq[String], task: ModuleTask, outputs: Seq[String], position: (Int, Int))

  case class WorkflowDataset(task: DatasetTask, position: (Int, Int))

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