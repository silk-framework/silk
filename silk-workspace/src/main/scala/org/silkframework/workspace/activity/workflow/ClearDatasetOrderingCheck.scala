package org.silkframework.workspace.activity.workflow

import org.silkframework.dataset.operations.ClearDatasetOperator
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project

import scala.util.control.NonFatal

/**
  * Detects 'Clear dataset' nodes whose execution order relative to other nodes writing to the same
  * dataset is undefined. The physical clear happens when the dataset node fed by the clear operator
  * executes; without an explicit order (a data/dependency path between the nodes, or output
  * priorities that order the two end nodes) the clear may run before or after the writes.
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
    val unordered = unorderedPairsIfValid(workflow, project)
    if (unordered.nonEmpty) {
      context.value.updateWith(_.copy(workflowWarnings = unordered.map(_.message)))
    }
  }

  /** [[unorderedPairs]], or empty for an invalid workflow structure (reported with a proper error elsewhere). */
  def unorderedPairsIfValid(workflow: Workflow, project: Project)
                           (implicit userContext: UserContext): Seq[UnorderedClearWrite] = {
    try {
      unorderedPairs(workflow, project)
    } catch {
      case NonFatal(_) => Seq.empty
    }
  }

  /**
    * Returns the clear/writer node pairs of the same dataset task with no user-defined execution order.
    * A pair is not reported if a (transitive) path connects the two nodes in either direction, if output
    * priorities order the two end nodes, or if clear and write happen on the same node (port order
    * decides there). May throw if the workflow graph is invalid (e.g. cyclic).
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
    val dependencyNodeById = workflow.dependencyNodesById
    val nodeById: Map[String, WorkflowNode] = workflow.nodes.map(n => n.nodeId -> n).toMap
    val datasetNodes = workflow.nodes.collect { case ds: WorkflowDataset => ds }.sortBy(_.nodeId)
    val tasksWithDataOutput = workflow.tasksWithDataOutput(project)

    def isWriter(node: WorkflowDataset): Boolean = {
      node.inputs.flatten.exists { inputId =>
        !clearOperatorNodeIds.contains(inputId) && nodeById.get(inputId).exists(n => tasksWithDataOutput.contains(n.task))
      }
    }
    def datasetLabel(taskId: Identifier): String =
      project.anyTaskOption(taskId).map(_.fullLabel).getOrElse(taskId.toString)

    // Priorities only order DAG end nodes, and only if they distinguish the pair
    // (exactly one set, or both set with different values).
    val endNodeIds: Set[String] = dag.endNodes.map(_.nodeId).toSet
    def priorityDefinesOrder(clearNode: WorkflowDataset, writer: WorkflowDataset): Boolean = {
      endNodeIds.contains(clearNode.nodeId) && endNodeIds.contains(writer.nodeId) &&
        ((clearNode.outputPriority, writer.outputPriority) match {
          case (Some(clearPrio), Some(writerPrio)) => clearPrio != writerPrio
          case (None, None) => false
          case _ => true
        })
    }

    for {
      clearNode <- datasetNodes
      if clearNode.inputs.flatten.exists(clearOperatorNodeIds.contains)
      writer <- datasetNodes
      if writer.task == clearNode.task && writer.nodeId != clearNode.nodeId && isWriter(writer)
      if !priorityDefinesOrder(clearNode, writer)
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
