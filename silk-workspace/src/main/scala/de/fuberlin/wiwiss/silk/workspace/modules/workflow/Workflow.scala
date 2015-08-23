package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.Workflow.{WorkflowDataset, WorkflowOperator}

import scala.xml.Node

case class Workflow(id: Identifier, operators: Seq[WorkflowOperator], datasets: Seq[WorkflowDataset]) {

  def toXML = {
    <Workflow id={id.toString} >{
      for(op <- operators) yield {
        <Operator
          posX={op.position._1.toString}
          posY={op.position._2.toString}
          task={op.task}
          inputs={op.inputs.mkString(",")}
          outputs={op.outputs.mkString(",")} />
      }
    }{
      for(ds <- datasets) yield {
        <Dataset
          posX={ds.position._1.toString}
          posY={ds.position._2.toString}
          task={ds.task} />
      }
    }</Workflow>
  }

}

object Workflow {

  def fromXML(xml: Node) = {
    val id = (xml \ "@id").text

    val operators =
      for(op <- xml \ "Operator") yield {
        WorkflowOperator(
          inputs = (op \ "@inputs").text.split(',').toSeq,
          task = (op \ "@task").text,
          outputs = (op \ "@outputs").text.split(',').toSeq,
          position = ((op \ "@posX").text.toInt, (op \ "@posY").text.toInt)
        )
      }

    val datasets =
      for(ds <- xml \ "Dataset") yield {
        WorkflowDataset(
          task = (ds \ "@task").text,
          position = ((ds \ "@posX").text.toInt, (ds \ "@posY").text.toInt)
        )
      }

    new Workflow(id, operators, datasets)
  }

  case class WorkflowOperator(inputs: Seq[String], task: String, outputs: Seq[String], position: (Int, Int))

  case class WorkflowDataset(task: String, position: (Int, Int))

}