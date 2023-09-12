package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow.taskIds

import scala.xml.{Node, Text}

/**
  * A workflow step.
  * Either a dataset or an operator (such as a transformation or linking task).
  */
sealed trait WorkflowNode {
  type NodeReference = String

  /**
    * The name of the project task that this workflow step relates to.
    */
  def task: Identifier

  /**
    * The names of the input nodes.
    */
  def inputs: Seq[NodeReference]

  /**
    * The names of the outputs nodes.
    */
  def outputs: Seq[NodeReference]

  /**
    * The position in the visual representation. (x, y) coordinates.
    */
  def position: (Int, Int)

  /**
    * The id that is referenced by the inputs and outputs values.
    * This is necessary since a task can be used multiple times in a workflow.
    */
  def nodeId: NodeReference

  /**
    * Workflow nodes with a smaller priority are executed first. A node with a defined priority is executed before a node without priority.
    * This only applies for output nodes, i.e. nodes that do not have any outgoing connections. For other nodes this has no effect.
    */
  def outputPriority: Option[Double]

  /** Allows to re-configure the config parameters of this workflow node with values output from other workflow nodes.
    * This is used to re-configure workflow tasks at workflow runtime. */
  def configInputs: Seq[NodeReference]

  def copyNode(task: Identifier = task,
               inputs: Seq[NodeReference] = inputs,
               outputs: Seq[NodeReference] = outputs,
               position: (Int, Int) = position,
               nodeId: NodeReference = nodeId,
               outputPriority: Option[Double] = outputPriority,
               configInputs: Seq[NodeReference] = configInputs): WorkflowNode = {
    this match {
      case wo: WorkflowOperator =>
        wo.copy(task = task, inputs = inputs, outputs = outputs, position = position, nodeId = nodeId, outputPriority = outputPriority)
      case wd: WorkflowDataset =>
        wd.copy(task = task, inputs = inputs, outputs = outputs, position = position, nodeId = nodeId, outputPriority = outputPriority)
    }
  }

  /** All nodes given in this list should be computed before this node. */
  def dependencyInputs: Seq[NodeReference]

  /** All nodes that input any kind of data into this node. */
  def allInputs: Seq[NodeReference] = (inputs ++ configInputs).distinct

  /** All nodes that are connected to this node with an incoming edge. */
  def allNodesWithIncomingEdges: Seq[NodeReference] = (allInputs ++ dependencyInputs).distinct
}

object WorkflowNode {
  def parseOutputPriority(op: Node): Option[Double] = {
    val node = op \ "@outputPriority"
    if (node.isEmpty) {
      None
    } else {
      Some(node.text.toDouble)
    }
  }

  def parseNodeId(op: Node, task: String): String = {
    val node = op \ "@id"
    if (node.isEmpty) {
      task
    } else {
      node.text
    }
  }
}

case class WorkflowOperator(inputs: Seq[WorkflowNode#NodeReference],
                            task: Identifier,
                            outputs: Seq[WorkflowNode#NodeReference],
                            errorOutputs: Seq[String],
                            position: (Int, Int),
                            nodeId: WorkflowNode#NodeReference,
                            outputPriority: Option[Double],
                            configInputs: Seq[WorkflowNode#NodeReference],
                            dependencyInputs: Seq[WorkflowNode#NodeReference]) extends WorkflowNode

object WorkflowOperator {
  implicit val workflowOperatorXmlFormat: XmlFormat[WorkflowOperator] = new XmlFormat[WorkflowOperator] {
    override def read(op: Node)(implicit readContext: ReadContext): WorkflowOperator = {
      val inputStr = (op \ "@inputs").text
      val outputStr = (op \ "@outputs").text
      val errorOutputStr = (op \ "@errorOutputs").text
      val configInputStr = (op \ "@configInputs").text
      val dependencyInputStr = (op \ "@dependencyInputs").text
      val task = (op \ "@task").text
      WorkflowOperator(
        inputs = if (inputStr.isEmpty) Seq.empty else inputStr.split(',').toSeq,
        task = task,
        outputs = if (outputStr.isEmpty) Seq.empty else outputStr.split(',').toSeq,
        errorOutputs = if (errorOutputStr.trim.isEmpty) Seq() else errorOutputStr.split(',').toSeq,
        position = (Math.round((op \ "@posX").text.toDouble).toInt, Math.round((op \ "@posY").text.toDouble).toInt),
        nodeId = WorkflowNode.parseNodeId(op, task),
        outputPriority = WorkflowNode.parseOutputPriority(op),
        configInputs = if (configInputStr.isEmpty) Seq.empty else configInputStr.split(',').toSeq,
        dependencyInputs = if (dependencyInputStr.isEmpty) Seq.empty else dependencyInputStr.split(',').toSeq
      )
    }

    override def write(op: WorkflowOperator)(implicit writeContext: WriteContext[Node]): Node = {
        <Operator
        posX={op.position._1.toString}
        posY={op.position._2.toString}
        task={op.task}
        inputs={op.inputs.mkString(",")}
        outputs={op.outputs.mkString(",")}
        errorOutputs={op.errorOutputs.mkString(",")}
        id={op.nodeId}
        outputPriority={op.outputPriority map (priority => Text(priority.toString))}
        configInputs={op.configInputs.mkString(",")}
        dependencyInputs={op.dependencyInputs.mkString(",")}/>
    }
  }
}

case class WorkflowDataset(inputs: Seq[WorkflowNode#NodeReference],
                           task: Identifier,
                           outputs: Seq[WorkflowNode#NodeReference],
                           position: (Int, Int),
                           nodeId: WorkflowNode#NodeReference,
                           outputPriority: Option[Double],
                           configInputs: Seq[WorkflowNode#NodeReference],
                           dependencyInputs: Seq[WorkflowNode#NodeReference]) extends WorkflowNode

object WorkflowDataset {
  implicit val workflowDatasetXmlFormat: XmlFormat[WorkflowDataset] = new XmlFormat[WorkflowDataset] {
    override def read(ds: Node)(implicit readContext: ReadContext): WorkflowDataset = {
      val inputs = taskIds((ds \ "@inputs").text)
      val outputs = taskIds((ds \ "@outputs").text)
      val configInputStr = (ds \ "@configInputs").text
      val dependencyInputStr = (ds \ "@dependencyInputs").text
      val task = (ds \ "@task").text
      WorkflowDataset(
        inputs = inputs,
        task = task,
        outputs = outputs,
        position = (Math.round((ds \ "@posX").text.toDouble).toInt, Math.round((ds \ "@posY").text.toDouble).toInt),
        nodeId = WorkflowNode.parseNodeId(ds, task),
        outputPriority = WorkflowNode.parseOutputPriority(ds),
        configInputs = if (configInputStr.isEmpty) Seq.empty else configInputStr.split(',').toSeq,
        dependencyInputs = if (dependencyInputStr.isEmpty) Seq.empty else dependencyInputStr.split(',').toSeq
      )
    }

    override def write(ds: WorkflowDataset)(implicit writeContext: WriteContext[Node]): Node = {
        <Dataset
        posX={ds.position._1.toString}
        posY={ds.position._2.toString}
        task={ds.task}
        inputs={ds.inputs.mkString(",")}
        outputs={ds.outputs.mkString(",")}
        id={ds.nodeId}
        outputPriority={ds.outputPriority map (priority => Text(priority.toString))}
        configInputs={ds.configInputs.mkString(",")}
        dependencyInputs={ds.dependencyInputs.mkString(",")}/>
    }
  }
}