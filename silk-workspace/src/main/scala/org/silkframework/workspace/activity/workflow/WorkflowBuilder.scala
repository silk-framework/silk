package org.silkframework.workspace.activity.workflow

import org.silkframework.util.Identifier

import scala.collection.mutable


/**
 * Utility object for building common workflow types.
 */
object WorkflowBuilder {

  /**
   * Creates a new workflow builder.
   */
  def create(): WorkflowBuilder = {
    new WorkflowBuilder()
  }

  /**
   * Creates a workflow that transforms an input to an output.
   */
  def transform(inputId: Identifier, taskId: Identifier, outputId: Identifier): Workflow = {
    create().dataset(inputId).operator(taskId).dataset(outputId).build()
  }
}

/**
 * A builder for creating workflows.
 */
class WorkflowBuilder {

  private var currentNode: Option[WorkflowNode] = None
  private val datasets = mutable.Buffer[WorkflowDataset]()
  private val operators = mutable.Buffer[WorkflowOperator]()
  private var replaceableInputs: Seq[String] = Seq.empty
  private var replaceableOutputs: Seq[String] = Seq.empty

  /**
   * Adds a new dataset and connects it to the previous node.
   */
  def dataset(id: Identifier): WorkflowBuilder = {
    val newDataset =
      WorkflowDataset(
        inputs = Seq(currentNode.map(_.nodeId)),
        task = id,
        outputs = Seq.empty,
        position = (0, 0),
        nodeId = id,
        outputPriority = None,
        configInputs = Seq.empty,
        dependencyInputs = Seq.empty
      )
    connect(newDataset)
    this
  }

  /**
   * Adds a new operator and connects it to the previous node.
   */
  def operator(id: Identifier): WorkflowBuilder = {
    val newOperator =
      WorkflowOperator(
        inputs = Seq(currentNode.map(_.nodeId)),
        task = id,
        outputs = Seq.empty,
        errorOutputs = Seq.empty,
        position = (0, 0),
        nodeId = id,
        outputPriority = None,
        configInputs = Seq.empty,
        dependencyInputs = Seq.empty
      )
    connect(newOperator)
    this
  }

  def replaceableInputs(ids: Seq[String]): WorkflowBuilder = {
    replaceableInputs = ids
    this
  }

  def replaceableOutputs(ids: Seq[String]): WorkflowBuilder = {
    replaceableOutputs = ids
    this
  }

  def build(): Workflow = {
    addCurrentNode()
    Workflow(
      operators = WorkflowOperatorsParameter(operators.toSeq),
      datasets = WorkflowDatasetsParameter(datasets.toSeq),
      replaceableInputs = replaceableInputs,
      replaceableOutputs = replaceableOutputs
    )
  }

  private def connect(node: WorkflowNode): Unit = {
    for(op <- currentNode) {
      currentNode = Some(op.copyNode(outputs = op.outputs :+ node.nodeId))
    }
    addCurrentNode()
    currentNode = Some(node)
  }

  private def addCurrentNode(): Unit = {
    currentNode match {
      case Some(node: WorkflowOperator) =>
        operators.append(node)
      case Some(node: WorkflowDataset) =>
        datasets.append(node)
      case None =>
    }
  }

}
