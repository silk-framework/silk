package org.silkframework.workspace.activity.workflow

/**
  * A workflow step.
  * Either a dataset or an operator (such as a transformation or linking task).
  */
sealed trait WorkflowNode {

  /**
    * The name of the project task that this workflow step relates to.
    */
  def task: String

  /**
    * The names of the input tasks.
    */
  def inputs: Seq[String]

  /**
    * The names of the outputs tasks.
    */
  def outputs: Seq[String]

  /**
    * The position in the visual representation. (x, y) coordinates.
    */
  //TODO extract to separate class
  def position: (Int, Int)

}

case class WorkflowOperator(inputs: Seq[String], task: String, outputs: Seq[String], errorOutputs: Seq[String], position: (Int, Int), id: String) extends WorkflowNode

case class WorkflowDataset(inputs: Seq[String], task: String, outputs: Seq[String], position: (Int, Int)) extends WorkflowNode
