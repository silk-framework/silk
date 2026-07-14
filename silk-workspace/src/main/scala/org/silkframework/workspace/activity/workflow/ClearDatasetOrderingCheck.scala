package org.silkframework.workspace.activity.workflow

import org.silkframework.dataset.operations.ClearDatasetOperator
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project

import scala.util.control.NonFatal

/**
  * Detects 'Clear dataset' nodes whose execution order relative to other nodes writing to the same
  * dataset is undefined. The physical clear happens when the dataset node fed by the clear operator
  * executes; without an explicit order (a data/dependency path between the nodes, or an output
  * priority on the clear node) the clear may run before or after the writes.
  */
object ClearDatasetOrderingCheck {

  /** A clear node and a writer node of the same dataset task with no user-defined execution order. */
  case class UnorderedClearWrite(datasetTask: Identifier,
                                 datasetLabel: String,
                                 clearNodeId: String,
                                 writerNodeId: String) {
    def message: String =
      s"Dataset '$datasetLabel' is cleared by 'Clear dataset' node '$clearNodeId', but node '$writerNodeId' " +
        "writes to it without a defined execution order. The clear may run before or after the write. " +
        "Connect the two nodes with a dependency edge to make the order explicit."
  }

  /** Puts a warning on the run's report when a clear node has no defined execution order relative to
    * other nodes writing to the same dataset (the clear may run before or after the writes). */
  def warnInReport(workflow: Workflow, project: Project, context: ActivityContext[WorkflowExecutionReport])
                  (implicit userContext: UserContext): Unit = {
    try {
      val unordered = unorderedPairs(workflow, project)
      if (unordered.nonEmpty) {
        context.value.updateWith(_.copy(workflowWarnings = unordered.map(_.message)))
      }
    } catch {
      case NonFatal(_) => // An invalid workflow structure fails later in the executor with a proper error.
    }
  }

  /**
    * Returns the clear/writer node pairs of the same dataset task with no user-defined execution order.
    * A pair is not reported if a (transitive) path connects the two nodes in either direction, if the
    * clear node carries an explicit output priority, or if clear and write happen on the same node
    * (port order decides there). May throw if the workflow graph is invalid (e.g. cyclic).
    */
  def unorderedPairs(workflow: Workflow, project: Project)
                    (implicit userContext: UserContext): Seq[UnorderedClearWrite] = {
    val clearOperatorNodeIds: Set[String] = workflow.nodes
      .collect { case op: WorkflowOperator => op }
      .filter(op => project.anyTaskOption(op.task).exists(_.data.isInstanceOf[ClearDatasetOperator]))
      .map(_.nodeId).toSet
    if (clearOperatorNodeIds.isEmpty) {
      Seq.empty
    } else {
      unorderedPairs(workflow, project, clearOperatorNodeIds)
    }
  }

  private def unorderedPairs(workflow: Workflow, project: Project, clearOperatorNodeIds: Set[String])
                            (implicit userContext: UserContext): Seq[UnorderedClearWrite] = {
    // Forces the dependency graph, which validates the workflow structure (dangling references, cycles).
    val dag = workflow.workflowDependencyGraph
    val dependencyNodeById: Map[String, WorkflowDependencyNode] =
      (dag.endNodes ++ dag.endNodes.flatMap(_.precedingNodesRecursively)).map(n => n.nodeId -> n).toMap
    val nodeById: Map[String, WorkflowNode] = workflow.nodes.map(n => n.nodeId -> n).toMap
    val datasetNodes = workflow.nodes.collect { case ds: WorkflowDataset => ds }
    // Tasks whose output produces real data (mirrors the filter of Workflow.outputDatasets).
    val tasksWithDataOutput: Set[Identifier] = workflow.nodes.map(_.task).distinct
      .filter(taskId => project.anyTaskOption(taskId).exists(_.outputPort.isDefined)).toSet

    def isWriter(node: WorkflowDataset): Boolean = {
      node.inputs.flatten.exists { inputId =>
        !clearOperatorNodeIds.contains(inputId) && nodeById.get(inputId).exists(n => tasksWithDataOutput.contains(n.task))
      }
    }
    def datasetLabel(taskId: Identifier): String =
      project.anyTaskOption(taskId).map(_.fullLabel).getOrElse(taskId.toString)

    for {
      clearNode <- datasetNodes.sortBy(_.nodeId)
      // An output priority on the clear node is a user-defined order and suppresses the warning.
      if clearNode.inputs.flatten.exists(clearOperatorNodeIds.contains) && clearNode.outputPriority.isEmpty
      writer <- datasetNodes.sortBy(_.nodeId)
      if writer.task == clearNode.task && writer.nodeId != clearNode.nodeId && isWriter(writer)
      clearDepNode = dependencyNodeById(clearNode.nodeId)
      writerDepNode = dependencyNodeById(writer.nodeId)
      // A path between the two nodes (either direction) is an explicit order.
      if !clearDepNode.precedingNodesRecursively.contains(writerDepNode) &&
        !writerDepNode.precedingNodesRecursively.contains(clearDepNode)
    } yield {
      UnorderedClearWrite(clearNode.task, datasetLabel(clearNode.task), clearNode.nodeId, writer.nodeId)
    }
  }
}
