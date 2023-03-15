package org.silkframework.workspace.activity.workflow

import org.silkframework.util.Identifier

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

  def allInputs: Seq[NodeReference] = (inputs ++ configInputs).distinct
}

case class WorkflowOperator(inputs: Seq[WorkflowNode#NodeReference],
                            task: Identifier,
                            outputs: Seq[WorkflowNode#NodeReference],
                            errorOutputs: Seq[String],
                            position: (Int, Int),
                            nodeId: WorkflowNode#NodeReference,
                            outputPriority: Option[Double],
                            configInputs: Seq[WorkflowNode#NodeReference]) extends WorkflowNode

case class WorkflowDataset(inputs: Seq[WorkflowNode#NodeReference],
                           task: Identifier,
                           outputs: Seq[WorkflowNode#NodeReference],
                           position: (Int, Int),
                           nodeId: WorkflowNode#NodeReference,
                           outputPriority: Option[Double],
                           configInputs: Seq[WorkflowNode#NodeReference]) extends WorkflowNode
