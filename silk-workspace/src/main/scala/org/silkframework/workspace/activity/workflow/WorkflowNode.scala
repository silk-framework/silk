package org.silkframework.workspace.activity.workflow

/**
  * A workflow step.
  * Either a dataset or an operator (such as a transformation or linking task).
  */
sealed trait WorkflowNode {
  type NodeReference = String

  /**
    * The name of the project task that this workflow step relates to.
    */
  def task: String

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
  //TODO extract to separate class
  def position: (Int, Int)

  /**
    * The id that is referenced by the inputs and outputs values.
    * This is necessary since a task can be used multiple times in a workflow.
    */
  def nodeId: NodeReference
}

case class WorkflowOperator(inputs: Seq[WorkflowNode#NodeReference], task: String, outputs: Seq[WorkflowNode#NodeReference], errorOutputs: Seq[String], position: (Int, Int), nodeId: WorkflowNode#NodeReference) extends WorkflowNode

case class WorkflowDataset(inputs: Seq[WorkflowNode#NodeReference], task: String, outputs: Seq[WorkflowNode#NodeReference], position: (Int, Int), nodeId: WorkflowNode#NodeReference) extends WorkflowNode
