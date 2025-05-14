package org.silkframework.workspace.activity.workflow

import org.silkframework.util.Identifier

/**
 * Utility object for building common workflow types.
 */
object WorkflowBuilder {

  /**
   * Creates a workflow that transforms an input to an output.
   */
  def transform(inputId: Identifier, taskId: Identifier, outputId: Identifier): Workflow = {
    val inputOp =
      WorkflowOperator(
        inputs = Seq.empty,
        task = inputId,
        outputs = Seq(taskId),
        errorOutputs = Seq.empty,
        position = (0, 0),
        nodeId = inputId,
        outputPriority = None,
        configInputs = Seq.empty,
        dependencyInputs = Seq.empty
      )

    // Add workflow operator
    val workflowOp =
      WorkflowOperator(
        inputs = Seq(Some(inputId)),
        task = taskId,
        outputs = Seq(outputId),
        errorOutputs = Seq.empty,
        position = (0, 0),
        nodeId = taskId,
        outputPriority = None,
        configInputs = Seq.empty,
        dependencyInputs = Seq.empty
      )

    // Add output operator
    val outputOp =
      WorkflowOperator(
        inputs = Seq(Some(taskId)),
        task = outputId,
        outputs = Seq.empty,
        errorOutputs = Seq.empty,
        position = (0, 0),
        nodeId = outputId,
        outputPriority = None,
        configInputs = Seq.empty,
        dependencyInputs = Seq.empty
      )

    // Create the workflow
    Workflow(WorkflowOperatorsParameter(Seq(inputOp, workflowOp, outputOp)))
  }

  /**
   * Creates a workflow that takes an input and produces an output.
   */
  def inputOutput(inputId: Identifier, outputId: Identifier): Workflow = {
    val inputOp =
      WorkflowOperator(
        inputs = Seq.empty,
        task = inputId,
        outputs = Seq(outputId),
        errorOutputs = Seq.empty,
        position = (0, 0),
        nodeId = inputId,
        outputPriority = None,
        configInputs = Seq.empty,
        dependencyInputs = Seq.empty
      )

    // Add workflow operator
    val outputOp =
      WorkflowOperator(
        inputs = Seq(Some(inputId)),
        task = outputId,
        outputs = Seq.empty,
        errorOutputs = Seq.empty,
        position = (0, 0),
        nodeId = outputId,
        outputPriority = None,
        configInputs = Seq.empty,
        dependencyInputs = Seq.empty
      )

    // Create the workflow
    Workflow(WorkflowOperatorsParameter(Seq(inputOp, outputOp)))
  }

}
