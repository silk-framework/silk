package org.silkframework.workspace.activity.workflow

import org.silkframework.config.TaskSpecification
import org.silkframework.util.Identifier

import scala.xml.Node

case class Workflow(id: Identifier, operators: Seq[WorkflowOperator], datasets: Seq[WorkflowDataset]) extends TaskSpecification {

  def nodes: Seq[WorkflowNode] = operators ++ datasets

  def node(name: String) = {
    nodes.find(_.task == name)
      .getOrElse(throw new NoSuchElementException(s"Cannot find node $name in the worklow."))
  }

  def toXML = {
    <Workflow id={id.toString} >{
      for(op <- operators) yield {
        <Operator
          posX={op.position._1.toString}
          posY={op.position._2.toString}
          task={op.task}
          inputs={op.inputs.mkString(",")}
          outputs={op.outputs.mkString(",")}
          errorOutputs={op.errorOutputs.mkString(",")}
          id={op.id}
          />
      }
    }{
      for(ds <- datasets) yield {
        <Dataset
          posX={ds.position._1.toString}
          posY={ds.position._2.toString}
          task={ds.task}
          inputs={ds.inputs.mkString(",")}
          outputs={ds.outputs.mkString(",")} />
      }
    }</Workflow>
  }

}

object Workflow {

  def fromXML(xml: Node) = {
    val id = (xml \ "@id").text

    val operators =
      for(op <- xml \ "Operator") yield {
        val inputStr = (op \ "@inputs").text
        val outputStr = (op \ "@outputs").text
        val errorOutputStr = (op \ "@errorOutputs").text
        WorkflowOperator(
          inputs = if(inputStr.isEmpty) Seq.empty else inputStr.split(',').toSeq,
          task = (op \ "@task").text,
          outputs = if(outputStr.isEmpty) Seq.empty else outputStr.split(',').toSeq,
          errorOutputs = if(errorOutputStr.trim.isEmpty) Seq() else errorOutputStr.split(',').toSeq,
          position = ((op \ "@posX").text.toInt, (op \ "@posY").text.toInt),
          id = {
            val node = ((op \ "@id"))
            if(node.isEmpty) {
              (op \ "@task").text
            } else {
              node.text
            }
          }
        )
      }

    val datasets =
      for(ds <- xml \ "Dataset") yield {
        val inputStr = (ds \ "@inputs").text
        val outputStr = (ds \ "@outputs").text
        WorkflowDataset(
          inputs = if(inputStr.isEmpty) Seq.empty else inputStr.split(',').toSeq,
          task = (ds \ "@task").text,
          outputs = if(outputStr.isEmpty) Seq.empty else outputStr.split(',').toSeq,
          position = ((ds \ "@posX").text.toInt, (ds \ "@posY").text.toInt)
        )
      }

    new Workflow(if(id.nonEmpty) Identifier(id) else Identifier.random, operators, datasets)
  }
}