package org.silkframework.workspace.changes

import org.silkframework.config.Task
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowDataset, WorkflowNode, WorkflowOperator}

/** One kind of edge between two workflow nodes. */
sealed trait WorkflowEdge {
  /** Names the kind in a description, e.g. " (dependency)"; empty for the plain data edge. */
  def suffix: String
}

object WorkflowEdge {

  /** A data-flow edge: the target sits on the source's outputs, the source on the target's positional
    * input ports. `ports` are the target ports the edge occupies; empty for a half-open edge that only
    * has the output side. */
  case class Data(ports: Seq[Int]) extends WorkflowEdge {
    override val suffix = ""
  }

  /** An execution-order dependency, held solely on the target's `dependencyInputs`. */
  case object Dependency extends WorkflowEdge {
    override val suffix = " (dependency)"
  }

  /** A config edge, held solely on the target's `configInputs`. */
  case object Config extends WorkflowEdge {
    override val suffix = " (config)"
  }

  /** An error-output edge, held solely on the source's `errorOutputs`; only operators have them. */
  case object Error extends WorkflowEdge {
    override val suffix = " (error output)"
  }
}

/**
  * Adds one node to a workflow's graph. The reverse of removing it, so it can also restore the
  * replaceable-dataset marking that removing the last node of a dataset task drops.
  */
case class AddWorkflowNode(taskId: Identifier, node: WorkflowNode,
                           replaceableInput: Boolean = false, replaceableOutput: Boolean = false,
                           override val taskLabel: Option[String] = None) extends TaskChange[Workflow] {

  override def describe: String = s"Added ${WorkflowNodeChanges.display(node)} to workflow '$taskName'"

  override def inverse: Option[RemoveWorkflowNode] =
    Some(RemoveWorkflowNode(taskId, node, replaceableInput, replaceableOutput, taskLabel))

  override def apply(workflow: Workflow): Workflow = {
    if(workflow.nodes.exists(_.nodeId == node.nodeId)) {
      throw ChangeConflictException(s"A node with id '${node.nodeId}' already exists in workflow '$taskName'.")
    }
    val added = node match {
      case operator: WorkflowOperator => workflow.copy(operators = workflow.operators.value :+ operator)
      case dataset: WorkflowDataset => workflow.copy(datasets = workflow.datasets.value :+ dataset)
    }
    def marked(ids: Seq[String], mark: Boolean): Seq[String] = if(mark) (ids :+ node.task.toString).distinct else ids
    added.copy(replaceableInputs = marked(added.replaceableInputs.taskIds, replaceableInput),
      replaceableOutputs = marked(added.replaceableOutputs.taskIds, replaceableOutput))
  }
}

/**
  * Removes one node from a workflow's graph. Applies only while the node is unchanged and no edge touches
  * it anymore; [[RemoveWorkflowNode.of]] captures the removal as the disconnection of every edge followed
  * by this change, so each entry can be reverted on its own.
  */
case class RemoveWorkflowNode(taskId: Identifier, node: WorkflowNode,
                              replaceableInput: Boolean = false, replaceableOutput: Boolean = false,
                              override val taskLabel: Option[String] = None) extends TaskChange[Workflow] {

  override def describe: String = s"Removed ${WorkflowNodeChanges.display(node)} from workflow '$taskName'"

  override def inverse: Option[AddWorkflowNode] =
    Some(AddWorkflowNode(taskId, node, replaceableInput, replaceableOutput, taskLabel))

  override def apply(workflow: Workflow): Workflow = {
    val current = WorkflowNodeChanges.expectNode(workflow, node.nodeId, taskName)
    if(current != node) {
      throw ChangeConflictException(s"Node '${node.nodeId}' in workflow '$taskName' has been changed since.")
    }
    val referenced = workflow.nodes.exists(other =>
      other.nodeId != node.nodeId && WorkflowNodeChanges.references(other, node.nodeId))
    if(referenced || WorkflowNodeChanges.connected(node)) {
      throw ChangeConflictException(s"Node '${node.nodeId}' in workflow '$taskName' is still connected.")
    }
    // Normalized, so the replaceable marking of a dataset task whose last node goes is dropped with it.
    Workflow.createNormalized(
      workflow.operators.value.filterNot(_.nodeId == node.nodeId),
      workflow.datasets.value.filterNot(_.nodeId == node.nodeId),
      workflow.uiAnnotations, workflow.replaceableInputs, workflow.replaceableOutputs)
  }
}

object RemoveWorkflowNode {

  /** The removal of a node as a request names it: the disconnection of each of its edges, then the removal
    * of the unconnected node. Applied as one batch; recorded as single changes, each revertible on its own. */
  def of(task: Task[Workflow], nodeId: String): Seq[TaskChange[Workflow]] = {
    val workflow = task.data
    val label = Change.capturedName(task)
    val node = WorkflowNodeChanges.requestedNode(workflow, nodeId, task.labelOrId)
    val disconnects =
      for(other <- workflow.nodes if other.nodeId != nodeId;
          (source, target, edge) <- WorkflowNodeChanges.edgesBetween(node, other)) yield {
        DisconnectWorkflowNodes(task.id, source, target, edge, label)
      }
    // The removal finds the node as the disconnections leave it, so it is captured from that state.
    val cleaned = disconnects.foldLeft(workflow)((current, disconnect) => disconnect(current))
    val remaining = cleaned.nodeById(nodeId)
    val lastOfTask = workflow.datasets.value.count(_.task == node.task) == 1
    disconnects :+ RemoveWorkflowNode(task.id, remaining,
      replaceableInput = lastOfTask && workflow.replaceableInputs.taskIds.contains(node.task.toString),
      replaceableOutput = lastOfTask && workflow.replaceableOutputs.taskIds.contains(node.task.toString),
      taskLabel = label)
  }
}

/** Adds one edge between two workflow nodes. Applies only while there is no such edge. */
case class ConnectWorkflowNodes(taskId: Identifier, sourceNodeId: String, targetNodeId: String,
                                edge: WorkflowEdge, override val taskLabel: Option[String] = None)
  extends TaskChange[Workflow] {

  override def describe: String = s"Connected '$sourceNodeId' to '$targetNodeId'${edge.suffix} in workflow '$taskName'"

  override def inverse: Option[DisconnectWorkflowNodes] =
    Some(DisconnectWorkflowNodes(taskId, sourceNodeId, targetNodeId, edge, taskLabel))

  override def apply(workflow: Workflow): Workflow = {
    WorkflowNodeChanges.applyToEdge(workflow, sourceNodeId, targetNodeId, taskName) { (source, target) =>
      edge match {
        case WorkflowEdge.Data(ports) =>
          if(ports.isEmpty && source.outputs.contains(targetNodeId)) {
            throw conflict("is already connected to")
          }
          val inputs = ports.foldLeft(target.inputs) { (inputs, port) =>
            val padded = if(port < inputs.length) inputs else inputs ++ Seq.fill(port - inputs.length + 1)(None)
            if(padded(port).isDefined) {
              throw ChangeConflictException(s"Input port $port of node '$targetNodeId' in workflow '$taskName' is already connected.")
            }
            padded.updated(port, Some(sourceNodeId))
          }
          (source.copyNode(outputs = (source.outputs :+ targetNodeId).distinct), target.copyNode(inputs = inputs))
        case WorkflowEdge.Dependency =>
          if(target.dependencyInputs.contains(sourceNodeId)) throw conflict("already depends on", flip = true)
          (source, WorkflowNodeChanges.withDependencyInputs(target, target.dependencyInputs :+ sourceNodeId))
        case WorkflowEdge.Config =>
          if(target.configInputs.contains(sourceNodeId)) throw conflict("is already connected to")
          (source, WorkflowNodeChanges.withConfigInputs(target, target.configInputs :+ sourceNodeId))
        case WorkflowEdge.Error =>
          val operator = WorkflowNodeChanges.expectOperator(source, taskName)
          if(operator.errorOutputs.contains(targetNodeId)) throw conflict("is already connected to")
          (operator.copy(errorOutputs = operator.errorOutputs :+ targetNodeId), target)
      }
    }
  }

  private def conflict(relation: String, flip: Boolean = false): ChangeConflictException = {
    val (a, b) = if(flip) (targetNodeId, sourceNodeId) else (sourceNodeId, targetNodeId)
    ChangeConflictException(s"Node '$a' $relation '$b' in workflow '$taskName'.")
  }
}

object ConnectWorkflowNodes {

  /** The data edge a request names, its port resolved: the given one, else the target's first vacant
    * input port, else appended. None if the edge already exists. */
  def data(task: Task[Workflow], sourceNodeId: String, targetNodeId: String, targetInputIndex: Option[Int]): Option[ConnectWorkflowNodes] = {
    val (source, target) = WorkflowNodeChanges.requestedEdgeNodes(task, sourceNodeId, targetNodeId)
    val connected = source.outputs.contains(targetNodeId)
    val ports = targetInputIndex match {
      case Some(index) =>
        if(index < 0) throw BadUserInputException("targetInputIndex must be >= 0.")
        target.inputs.lift(index).flatten match {
          case Some(occupant) if occupant != sourceNodeId =>
            throw BadUserInputException(s"Input port $index of the target is already connected to '$occupant'.")
          case Some(_) => Seq.empty // this port already references the source
          case None => Seq(index)
        }
      case None if target.inputs.contains(Some(sourceNodeId)) => Seq.empty // some port already references the source
      case None => Seq(target.inputs.indexOf(None) match {
        case -1 => target.inputs.length
        case vacant => vacant
      })
    }
    if(connected && ports.isEmpty) None // fully connected already — idempotent
    else Some(ConnectWorkflowNodes(task.id, sourceNodeId, targetNodeId, WorkflowEdge.Data(ports), Change.capturedName(task)))
  }

  /** The dependency edge a request names, or None if it already exists. */
  def dependency(task: Task[Workflow], sourceNodeId: String, targetNodeId: String): Option[ConnectWorkflowNodes] = {
    val (_, target) = WorkflowNodeChanges.requestedEdgeNodes(task, sourceNodeId, targetNodeId)
    if(target.dependencyInputs.contains(sourceNodeId)) None
    else Some(ConnectWorkflowNodes(task.id, sourceNodeId, targetNodeId, WorkflowEdge.Dependency, Change.capturedName(task)))
  }
}

/** Removes one edge between two workflow nodes. Applies only while the edge is as captured. */
case class DisconnectWorkflowNodes(taskId: Identifier, sourceNodeId: String, targetNodeId: String,
                                   edge: WorkflowEdge, override val taskLabel: Option[String] = None)
  extends TaskChange[Workflow] {

  override def describe: String = s"Disconnected '$sourceNodeId' from '$targetNodeId'${edge.suffix} in workflow '$taskName'"

  override def inverse: Option[ConnectWorkflowNodes] =
    Some(ConnectWorkflowNodes(taskId, sourceNodeId, targetNodeId, edge, taskLabel))

  override def apply(workflow: Workflow): Workflow = {
    WorkflowNodeChanges.applyToEdge(workflow, sourceNodeId, targetNodeId, taskName) { (source, target) =>
      edge match {
        case WorkflowEdge.Data(ports) =>
          if(ports.isEmpty && !source.outputs.contains(targetNodeId)) throw notConnected()
          val inputs = ports.foldLeft(target.inputs) { (inputs, port) =>
            if(!inputs.lift(port).flatten.contains(sourceNodeId)) {
              throw ChangeConflictException(s"Input port $port of node '$targetNodeId' in workflow '$taskName' is not connected to '$sourceNodeId'.")
            }
            inputs.updated(port, None)
          }
          (source.copyNode(outputs = source.outputs.filterNot(_ == targetNodeId)),
            target.copyNode(inputs = inputs.reverse.dropWhile(_.isEmpty).reverse))
        case WorkflowEdge.Dependency =>
          if(!target.dependencyInputs.contains(sourceNodeId)) throw notConnected()
          (source, WorkflowNodeChanges.withDependencyInputs(target, target.dependencyInputs.filterNot(_ == sourceNodeId)))
        case WorkflowEdge.Config =>
          if(!target.configInputs.contains(sourceNodeId)) throw notConnected()
          (source, WorkflowNodeChanges.withConfigInputs(target, target.configInputs.filterNot(_ == sourceNodeId)))
        case WorkflowEdge.Error =>
          val operator = WorkflowNodeChanges.expectOperator(source, taskName)
          if(!operator.errorOutputs.contains(targetNodeId)) throw notConnected()
          (operator.copy(errorOutputs = operator.errorOutputs.filterNot(_ == targetNodeId)), target)
      }
    }
  }

  private def notConnected(): ChangeConflictException =
    ChangeConflictException(s"Node '$sourceNodeId' is not connected to '$targetNodeId'${edge.suffix} in workflow '$taskName'.")
}

object DisconnectWorkflowNodes {

  /** The removal of what connects two nodes, as a request names it: the data edge (all ports referencing
    * the source, or just the given one) and/or the dependency edge — none of each that does not exist. */
  def of(task: Task[Workflow], sourceNodeId: String, targetNodeId: String,
         data: Boolean, dependency: Boolean, targetInputIndex: Option[Int] = None): Seq[DisconnectWorkflowNodes] = {
    val (source, target) = WorkflowNodeChanges.requestedEdgeNodes(task, sourceNodeId, targetNodeId)
    val label = Change.capturedName(task)
    val dataEdge = if(!data) None else {
      val ports = targetInputIndex match {
        case Some(index) =>
          if(index < 0) throw BadUserInputException("targetInputIndex must be >= 0.")
          Seq(index).filter(i => target.inputs.lift(i).flatten.contains(sourceNodeId))
        case None =>
          target.inputs.zipWithIndex.collect { case (Some(id), index) if id == sourceNodeId => index }
      }
      if(ports.isEmpty && !source.outputs.contains(targetNodeId)) None
      else Some(DisconnectWorkflowNodes(task.id, sourceNodeId, targetNodeId, WorkflowEdge.Data(ports), label))
    }
    val dependencyEdge =
      if(dependency && target.dependencyInputs.contains(sourceNodeId)) {
        Some(DisconnectWorkflowNodes(task.id, sourceNodeId, targetNodeId, WorkflowEdge.Dependency, label))
      } else {
        None
      }
    dataEdge.toSeq ++ dependencyEdge
  }
}

private object WorkflowNodeChanges {

  /** Names the node for display, with its task when the node id does not name it already. */
  def display(node: WorkflowNode): String = {
    val kind = node match {
      case _: WorkflowDataset => "dataset"
      case _: WorkflowOperator => "operator"
    }
    val task = if(node.task.toString != node.nodeId) s" (task '${node.task}')" else ""
    s"$kind node '${node.nodeId}'$task"
  }

  /** The node a request names; that it is missing is the user's error, unlike at apply time. */
  def requestedNode(workflow: Workflow, nodeId: String, taskName: String): WorkflowNode = {
    workflow.nodes.find(_.nodeId == nodeId)
      .getOrElse(throw new NotFoundException(s"No node '$nodeId' found in workflow '$taskName'."))
  }

  /** Both ends of an edge a request names. */
  def requestedEdgeNodes(task: Task[Workflow], sourceNodeId: String, targetNodeId: String): (WorkflowNode, WorkflowNode) = {
    (requestedNode(task.data, sourceNodeId, task.labelOrId), requestedNode(task.data, targetNodeId, task.labelOrId))
  }

  /** The node with the given id at apply time. */
  def expectNode(workflow: Workflow, nodeId: String, taskName: String): WorkflowNode = {
    workflow.nodes.find(_.nodeId == nodeId)
      .getOrElse(throw ChangeConflictException(s"No node '$nodeId' found in workflow '$taskName'."))
  }

  /** Applies an update to both ends of an edge and replaces them in the workflow. */
  def applyToEdge(workflow: Workflow, sourceNodeId: String, targetNodeId: String, taskName: String)
                 (update: (WorkflowNode, WorkflowNode) => (WorkflowNode, WorkflowNode)): Workflow = {
    require(sourceNodeId != targetNodeId, "A workflow node cannot be connected to itself.")
    val (newSource, newTarget) = update(expectNode(workflow, sourceNodeId, taskName), expectNode(workflow, targetNodeId, taskName))
    val replaced = Map(sourceNodeId -> newSource, targetNodeId -> newTarget)
    workflow.copy(
      operators = workflow.operators.value.map(op => replaced.get(op.nodeId).collect { case operator: WorkflowOperator => operator }.getOrElse(op)),
      datasets = workflow.datasets.value.map(ds => replaced.get(ds.nodeId).collect { case dataset: WorkflowDataset => dataset }.getOrElse(ds)))
  }

  /** The edges between the two nodes, in either direction, as (source, target, edge). */
  def edgesBetween(node: WorkflowNode, other: WorkflowNode): Seq[(String, String, WorkflowEdge)] = {
    def dataEdge(source: WorkflowNode, target: WorkflowNode): Option[(String, String, WorkflowEdge)] = {
      val ports = target.inputs.zipWithIndex.collect { case (Some(id), index) if id == source.nodeId => index }
      if(ports.nonEmpty || source.outputs.contains(target.nodeId)) Some((source.nodeId, target.nodeId, WorkflowEdge.Data(ports)))
      else None
    }
    // A dependency or config edge is held on the target, an error edge on the source.
    def onTarget(source: WorkflowNode, target: WorkflowNode, held: WorkflowNode => Seq[String], edge: WorkflowEdge) = {
      if(held(target).contains(source.nodeId)) Some((source.nodeId, target.nodeId, edge)) else None
    }
    def errorEdge(source: WorkflowNode, target: WorkflowNode) = {
      if(errorOutputs(source).contains(target.nodeId)) Some((source.nodeId, target.nodeId, WorkflowEdge.Error)) else None
    }
    (dataEdge(other, node) ++ dataEdge(node, other) ++
      onTarget(other, node, _.dependencyInputs, WorkflowEdge.Dependency) ++ onTarget(node, other, _.dependencyInputs, WorkflowEdge.Dependency) ++
      onTarget(other, node, _.configInputs, WorkflowEdge.Config) ++ onTarget(node, other, _.configInputs, WorkflowEdge.Config) ++
      errorEdge(node, other) ++ errorEdge(other, node)).toSeq
  }

  /** Whether the node holds an edge naming `nodeId`. */
  def references(node: WorkflowNode, nodeId: String): Boolean = heldEdges(node).contains(nodeId)

  /** Whether the node holds an edge to another node; a self-loop travels with the node itself. */
  def connected(node: WorkflowNode): Boolean = heldEdges(node).exists(_ != node.nodeId)

  private def heldEdges(node: WorkflowNode): Seq[String] = {
    node.inputs.flatten ++ node.outputs ++ node.configInputs ++ node.dependencyInputs ++ errorOutputs(node)
  }

  def errorOutputs(node: WorkflowNode): Seq[String] = node match {
    case operator: WorkflowOperator => operator.errorOutputs
    case _ => Seq.empty
  }

  def withDependencyInputs(node: WorkflowNode, dependencyInputs: Seq[String]): WorkflowNode = node match {
    case operator: WorkflowOperator => operator.copy(dependencyInputs = dependencyInputs)
    case dataset: WorkflowDataset => dataset.copy(dependencyInputs = dependencyInputs)
  }

  def withConfigInputs(node: WorkflowNode, configInputs: Seq[String]): WorkflowNode = node match {
    case operator: WorkflowOperator => operator.copy(configInputs = configInputs)
    case dataset: WorkflowDataset => dataset.copy(configInputs = configInputs)
  }

  def expectOperator(node: WorkflowNode, taskName: String): WorkflowOperator = node match {
    case operator: WorkflowOperator => operator
    case _ => throw ChangeConflictException(s"Node '${node.nodeId}' in workflow '$taskName' is a dataset and has no error outputs.")
  }
}
