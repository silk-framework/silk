package org.silkframework.workspace.activity.workflow

import org.silkframework.config.TaskSpec
import org.silkframework.dataset.{Dataset, VariableDataset}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}

import scala.xml.{Elem, Node, Text}

/**
  * A workflow is a DAG, whose nodes are either datasets or operators and specifies the data flow between them.
  *
  * @param operators Operators, e.g. transformations and link specs.
  * @param datasets
  */
case class Workflow(operators: Seq[WorkflowOperator], datasets: Seq[WorkflowDataset]) extends TaskSpec {

  lazy val nodes: Seq[WorkflowNode] = operators ++ datasets

  def nodeById(nodeId: String): WorkflowNode = {
    nodes.find(_.nodeId == nodeId)
        .getOrElse(throw new NoSuchElementException(s"Cannot find node $nodeId in the workflow."))
  }

  /**
    * A topologically sorted sequence of [[WorkflowOperator]] used in this workflow with the layer index, i.e.
    * in which layer this operator would be executed.
    */
  lazy val topologicalSortedNodesWithLayerIndex: IndexedSeq[(WorkflowNode, Int)] = {
    val inputs = inputWorkflowNodeIds()
    val outputs = outputWorkflowNodeIds()
    val pureOutputNodes = outputs.toSet -- inputs
    var done = pureOutputNodes
    var sortedOperators = Vector.empty[(WorkflowNode, Int)]
    val (start, rest) = nodes.partition(node => pureOutputNodes.contains(node.nodeId))
    var layer = 1
    sortedOperators ++= start.map((_, layer))
    var operatorsToSort = rest
    while (operatorsToSort.nonEmpty) {
      layer += 1
      val (satisfied, unsatisfied) = operatorsToSort.partition(op => op.inputs.forall(done))
      if (satisfied.isEmpty) {
        throw new RuntimeException("Cannot topologically sort operators in workflow!")
      }
      sortedOperators ++= satisfied.map((_, layer))
      done ++= satisfied.map(_.nodeId)
      operatorsToSort = unsatisfied
    }
    sortedOperators
  }

  lazy val topologicalSortedNodes: Seq[WorkflowNode] = topologicalSortedNodesWithLayerIndex.map(_._1)

  /**
    * Returns a dependency graph that can be traversed from the start or end nodes and consists of
    * double linked nodes.
    *
    * The end nodes are sorted (ASC) by output priority.
    */
  lazy val workflowDependencyGraph: WorkflowDependencyGraph = {
    // Test if this graph can be topologically sorted
    topologicalSortedNodes
    val inputs = inputWorkflowNodeIds()
    val outputs = outputWorkflowNodeIds()
    val startNodes = outputs.toSet -- inputs
    val isolatedNodes = singleWorkflowNodes()
    val endNodes = (inputs.toSet -- outputs) ++ isolatedNodes
    val workflowNodeMap: Map[String, WorkflowDependencyNode] = constructNodeMap
    val startDependencyNodes = startNodes.map(workflowNodeMap)
    val endDependencyNodes = sortWorkflowNodesByOutputPriority(endNodes.map(workflowNodeMap).toSeq)
    WorkflowDependencyGraph(startDependencyNodes, endDependencyNodes)
  }

  def sortWorkflowNodesByOutputPriority(nodes: Seq[WorkflowDependencyNode]): Seq[WorkflowDependencyNode] = {
    nodes.sortWith { case (left, right) =>
      (left.workflowNode.outputPriority, right.workflowNode.outputPriority) match {
        case (None, None) =>
          left.nodeId < right.nodeId
        case (Some(_), None) =>
          true
        case (None, Some(_)) =>
          false
        case (Some(leftPrio), Some(rightPrio)) =>
          leftPrio <= rightPrio
      }
    }
  }

  private def constructNodeMap: Map[String, WorkflowDependencyNode] = {
    val workflowNodeMap = nodes.map(n => (n.nodeId, WorkflowDependencyNode(n))).toMap
    for (node <- nodes) {
      val depNode = workflowNodeMap(node.nodeId)
      for (inputNode <- node.inputs) {
        val precedingNode = workflowNodeMap.getOrElse(inputNode,
          throw new scala.RuntimeException("Unsatisfiable input dependency in workflow! Dependency: " + inputNode))
        depNode.addPrecedingNode(precedingNode)
        precedingNode.addFollowingNode(depNode)
      }
      for (outputNode <- node.outputs) {
        val followingNode = workflowNodeMap.getOrElse(outputNode,
          throw new scala.RuntimeException("Unsatisfiable output dependency in workflow! Dependency: " + outputNode))
        depNode.addFollowingNode(followingNode)
        followingNode.addPrecedingNode(depNode)
      }
    }
    // Make immutable
    workflowNodeMap.foreach(_._2.setToImmutable())
    workflowNodeMap
  }

  /**
    * Returns all variable datasets and how they are used in the workflow.
    *
    * @param project
    * @return
    * @throws Exception if a variable dataset is used as input and output, which is not allowed.
    */
  def variableDatasets(project: Project): AllVariableDatasets = {
    val variableDatasetsUsedInOutput =
      for (datasetTask <- outputDatasets(project)
           if datasetTask.data.isInstanceOf[VariableDataset]) yield {
        datasetTask.id.toString
      }

    val variableDatasetsUsedInInput =
      for (datasetTask <- inputDatasets(project)
           if datasetTask.data.isInstanceOf[VariableDataset]) yield {
        datasetTask.id.toString
      }
    val bothInAndOut = variableDatasetsUsedInInput.toSet & variableDatasetsUsedInOutput.toSet
    if (bothInAndOut.nonEmpty) {
      throw new scala.Exception("Cannot use variable dataset as input AND output! Affected datasets: " + bothInAndOut.mkString(", "))
    }
    AllVariableDatasets(variableDatasetsUsedInInput, variableDatasetsUsedInOutput)
  }

  /** Returns all Dataset tasks that are used as input in the workflow */
  def inputDatasets(project: Project): Seq[ProjectTask[Dataset]] = {
    for (datasetNodeId <- operators.flatMap(_.inputs).distinct;
         dataset <- project.taskOption[Dataset](nodeById(datasetNodeId).task)) yield {
      dataset
    }
  }

  /** Returns all Dataset tasks that are uesd as output in the workflow */
  def outputDatasets(project: Project): Seq[ProjectTask[Dataset]] = {
    for (datasetNodeId <- operators.flatMap(_.outputs).distinct;
         dataset <- project.taskOption[Dataset](nodeById(datasetNodeId).task)) yield {
      dataset
    }
  }

  /** Returns node ids of workflow nodes that have inputs from other nodes */
  def inputWorkflowNodeIds(): Seq[String] = {
    val outputs = nodes.flatMap(_.outputs).distinct
    val nodesWithInputs = nodes.filter(_.inputs.size > 0).map(_.nodeId)
    (outputs ++ nodesWithInputs).distinct
  }

  /** Returns node ids of workflow nodes that have neither inputs nor outputs */
  def singleWorkflowNodes(): Seq[String] = {
    nodes.filter(n => n.inputs.isEmpty && n.outputs.isEmpty).map(_.nodeId)
  }

  /** Returns node ids of workflow nodes that output data into other nodes */
  def outputWorkflowNodeIds(): Seq[String] = {
    val inputs = nodes.flatMap(_.inputs).distinct
    val nodesWithOutputs = nodes.filter(_.outputs.nonEmpty).map(_.nodeId)
    (inputs ++ nodesWithOutputs).distinct
  }

  case class AllVariableDatasets(dataSources: Seq[String], sinks: Seq[String])

  case class WorkflowDependencyGraph(startNodes: Iterable[WorkflowDependencyNode],
                                     endNodes: Seq[WorkflowDependencyNode])

  /**
    * A workflow does not have any inputs.
    */
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = Some(Seq())

  /**
    * The schema of the output data.
    * Returns None, if the schema is unknown or if no output is written by this task.
    */
  override def outputSchemaOpt: Option[EntitySchema] = None

  /**
    * The tasks that are referenced by this workflow.
    */
  override def referencedTasks: Set[Identifier] = (operators ++ datasets).map(_.task).toSet

  /**
    * Returns this workflow with position parameters of all workflow operators being set automatically by a layout algorithm.
    */
  def autoLayout(layoutConfig: WorkflowLayoutConfig): Workflow = {
    val operatorsByLayer = topologicalSortedNodesWithLayerIndex.groupBy(_._2)
    val operatorsAutoPositioned = for ((layerNr, operators) <- operatorsByLayer) yield {
      autoLayoutWorkflowNodes(operators.map(_._1), layerNr, layoutConfig: WorkflowLayoutConfig)
    }
    val (workflowDatasets, workflowOperators) = operatorsAutoPositioned.flatten.toSeq.partition(_.isInstanceOf[WorkflowDataset])
    this.copy(datasets = workflowDatasets.map(_.asInstanceOf[WorkflowDataset]), operators = workflowOperators.map(_.asInstanceOf[WorkflowOperator]))
  }

  // Create workflow element for frontend model and set its layout
  private def autoLayoutWorkflowNodes(workflowOperators: Seq[WorkflowNode],
                                      layerNr: Int,
                                      layoutConfig: WorkflowLayoutConfig): Seq[WorkflowNode] = {
    def calculateElementPosition(elementIndexInLayer: Int): (Int, Int) = {
      import layoutConfig._

      val xPosition = offsetX + layerNr * (elementWidth + horizontalPadding)
      val yPosition = offsetY + elementIndexInLayer * (elementHeight + verticalPadding)
      (xPosition, yPosition)
    }

    for ((element, index) <- workflowOperators.zipWithIndex) yield {
      val newPosition = calculateElementPosition(index)
      element.copyNode(position = newPosition)
    }
  }
}

object Workflow {

  implicit object TransformSpecificationFormat extends XmlFormat[Workflow] {
    /**
      * Deserialize a value from XML.
      */
    override def read(xml: Node)(implicit readContext: ReadContext): Workflow = {
      val operators =
        for (op <- xml \ "Operator") yield {
          val inputStr = (op \ "@inputs").text
          val outputStr = (op \ "@outputs").text
          val errorOutputStr = (op \ "@errorOutputs").text
          val task = (op \ "@task").text
          WorkflowOperator(
            inputs = if (inputStr.isEmpty) Seq.empty else inputStr.split(',').toSeq,
            task = task,
            outputs = if (outputStr.isEmpty) Seq.empty else outputStr.split(',').toSeq,
            errorOutputs = if (errorOutputStr.trim.isEmpty) Seq() else errorOutputStr.split(',').toSeq,
            position = (Math.round((op \ "@posX").text.toDouble).toInt, Math.round((op \ "@posY").text.toDouble).toInt),
            nodeId = parseNodeId(op, task),
            outputPriority = parseOutputPriority(op)
          )
        }

      val datasets =
        for (ds <- xml \ "Dataset") yield {
          val inputStr = (ds \ "@inputs").text
          val outputStr = (ds \ "@outputs").text
          val task = (ds \ "@task").text
          WorkflowDataset(
            inputs = if (inputStr.isEmpty) Seq.empty else inputStr.split(',').toSeq,
            task = task,
            outputs = if (outputStr.isEmpty) Seq.empty else outputStr.split(',').toSeq,
            position = (Math.round((ds \ "@posX").text.toDouble).toInt, Math.round((ds \ "@posY").text.toDouble).toInt),
            nodeId = parseNodeId(ds, task),
            outputPriority = parseOutputPriority(ds)
          )
        }

      new Workflow(operators, datasets)
    }

    /**
      * Serialize a value to XML.
      */
    override def write(workflow: Workflow)(implicit writeContext: WriteContext[Node]): Node = {
      import workflow._
      <Workflow>
        {for (op <- operators) yield {
          <Operator
          posX={op.position._1.toString}
          posY={op.position._2.toString}
          task={op.task}
          inputs={op.inputs.mkString(",")}
          outputs={op.outputs.mkString(",")}
          errorOutputs={op.errorOutputs.mkString(",")}
          id={op.nodeId}
          outputPriority={op.outputPriority map (priority => Text(priority.toString))}/>
      }}{for (ds <- datasets) yield {
          <Dataset
          posX={ds.position._1.toString}
          posY={ds.position._2.toString}
          task={ds.task}
          inputs={ds.inputs.mkString(",")}
          outputs={ds.outputs.mkString(",")}
          id={ds.nodeId}
          outputPriority={ds.outputPriority map (priority => Text(priority.toString))}/>
      }}
      </Workflow>
    }

    private def parseOutputPriority(op: Node): Option[Double] = {
      val node = ((op \ "@outputPriority"))
      if (node.isEmpty) {
        None
      } else {
        Some(node.text.toDouble)
      }
    }

    private def parseNodeId(op: Node, task: String): String = {
      val node = ((op \ "@id"))
      if (node.isEmpty) {
        task
      } else {
        node.text
      }
    }
  }
}

/**
  * Since this class is spanning a double linked graph, this node needs to be mutable
  * until the graph has been constructed. Afterwards the node is set to immutable and cannot
  * be changed anymore.
  *
  * @param workflowNode
  */
case class WorkflowDependencyNode(workflowNode: WorkflowNode) {
  private var mutableNode = true

  private var _precedingNodes = Set.empty[WorkflowDependencyNode]
  private var _followingNodes = Set.empty[WorkflowDependencyNode]

  def setToImmutable(): Unit = {
    mutableNode = false
  }

  def nodeId: String = workflowNode.nodeId

  def isMutable: Boolean = mutableNode

  def addPrecedingNode(node: WorkflowDependencyNode): Unit = {
    if (isMutable) {
      _precedingNodes += node
    } else {
      throw new IllegalStateException("Cannot add node to preceding nodes! This node is set to immutable!")
    }
  }

  def addFollowingNode(node: WorkflowDependencyNode): Unit = {
    if (isMutable) {
      _followingNodes += node
    } else {
      throw new IllegalStateException("Cannot add node to following nodes! This node is set to immutable!")
    }
  }

  def followingNodes: Set[WorkflowDependencyNode] = _followingNodes

  def precedingNodes: Set[WorkflowDependencyNode] = _precedingNodes

  def inputNodes: Seq[WorkflowDependencyNode] = {
    for (
      input <- workflowNode.inputs;
      pNode <- precedingNodes.filter(_.nodeId == input)) yield {
      pNode
    }
  }
}