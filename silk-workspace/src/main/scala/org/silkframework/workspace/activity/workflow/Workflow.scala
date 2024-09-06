package org.silkframework.workspace.activity.workflow

import org.silkframework.config.{FlexibleNumberOfInputs, InputPorts, Port, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetSpec, VariableDataset}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.{AnyPlugin, PluginObjectParameterNoSchema}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}
import org.silkframework.workspace.{Project, ProjectTask}

import scala.collection.mutable
import scala.language.implicitConversions
import scala.xml.{Node, Text}

/**
  * A workflow is a DAG, whose nodes are either datasets or operators and specifies the data flow between them.
  *
  * @param operators Operators, e.g. transformations and link specs.
  * @param datasets
  */
@Plugin(
  id = "workflow",
  label = "Workflow",
  categories = Array("Workflow"),
  description =
      """A workflow describes a directed data processing pipeline bringing together datasets and processing tasks."""
)
case class Workflow(@Param(label = "Workflow operators", value = "Workflow operators process input data or access external non-dataset services.", visibleInDialog = false)
                    operators: WorkflowOperatorsParameter = WorkflowOperatorsParameter(Seq.empty),
                    @Param(label = "Workflow datasets", value = "Workflow datasets allow reading and writing data from/to a data source/sink.", visibleInDialog = false)
                    datasets: WorkflowDatasetsParameter = WorkflowDatasetsParameter(Seq.empty),
                    @Param(label = "UI annotations", value = "Annotations that are displayed in the workflow editor to describe parts of the workflow.", visibleInDialog = false)
                    uiAnnotations: UiAnnotations = UiAnnotations(),
                    @Param(label = "Replaceable input datasets", value = "The IDs of input datasets that can be replaced in the workflow with other user defined datasets.", visibleInDialog = false)
                    replaceableInputs: TaskIdentifierParameter = TaskIdentifierParameter(Seq.empty),
                    @Param(label = "Replaceable output datasets", value = "The IDs of output datasets that can be replaced in the workflow with other user defined datasets.", visibleInDialog = false)
                    replaceableOutputs: TaskIdentifierParameter = TaskIdentifierParameter(Seq.empty)) extends TaskSpec with AnyPlugin {

  lazy val nodes: Seq[WorkflowNode] = operators ++ datasets

  def nodeById(nodeId: String): WorkflowNode = {
    nodes.find(_.nodeId == nodeId)
        .getOrElse(throw new NoSuchElementException(s"Cannot find node $nodeId in the workflow."))
  }

  def validate(): AllReplaceableDatasets = {
    // We do not have the project here, so we can only validate the replaceable datasets of this workflow and not of nested workflows
    validateAndGetReplaceableDatasetsOfCurrentWorkflow()
  }
  validate()

  /**
    * A topologically sorted sequence of [[WorkflowOperator]] used in this workflow with the layer index, i.e.
    * in which layer this operator would be executed.
    */
  private lazy val topologicalSortedNodesWithLayerIndex: IndexedSeq[(WorkflowNode, Int)] = {
    val inputs = inputWorkflowNodeIds()
    val outputs = outputWorkflowNodeIds()
    val pureOutputNodes = outputs.toSet -- inputs
    var done = pureOutputNodes
    var sortedOperators = Vector.empty[(WorkflowNode, Int)]
    val (start, rest) = nodes.toList.partition(node => pureOutputNodes.contains(node.nodeId))
    var layer = 1
    sortedOperators ++= start.map((_, layer))
    var operatorsToSort = rest
    while (operatorsToSort.nonEmpty) {
      layer += 1
      val (satisfied, unsatisfied) = operatorsToSort.partition(op => op.allIncomingNodes.forall(done))
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
    val startDependencyNodes = startNodes.toSeq.map(workflowNodeMap).sortBy(_.nodeId)
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
      for (inputNode <- node.allIncomingNodes) {
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

  // Returns and validates the replaceable datasets of this workflow that are actually in use, others are ignored.
  private def validateAndGetReplaceableDatasetsOfCurrentWorkflow(): AllReplaceableDatasets = {
    val datasetNodeMap = datasets.map(d => d.nodeId -> d.task.toString).toMap
    val workflowDatasetOutputs = operators.flatMap(_.outputs.flatMap(datasetNodeMap.get)).distinct.toSet
    val workflowDatasetInputs = operators.flatMap(_.inputs.flatten.flatMap(datasetNodeMap.get)).distinct.toSet
    val replaceableInputUsedAsOutput = workflowDatasetOutputs.intersect(replaceableInputs.taskIds.toSet)
    if (replaceableInputUsedAsOutput.nonEmpty) {
      throw new IllegalArgumentException("Datasets marked as replaceable input must not be used as output dataset! Affected dataset: " + replaceableInputUsedAsOutput.mkString(", "))
    }
    val replaceableOutputUsedAsInput = workflowDatasetInputs.intersect(replaceableOutputs.taskIds.toSet)
    if (replaceableOutputUsedAsInput.nonEmpty) {
      throw new IllegalArgumentException("Datasets marked as replaceable input must not be used as output dataset! Affected dataset: " + replaceableOutputUsedAsInput.mkString(", "))
    }
    val bothInputAndOutput = (replaceableInputs ++ replaceableOutputs).filter(id =>
      workflowDatasetInputs.contains(id) && workflowDatasetOutputs.contains(id)
    )
    if (bothInputAndOutput.nonEmpty) {
      throw new IllegalArgumentException("Datasets must not be marked as replaceable input and output simultaneously! Affected dataset: " + bothInputAndOutput.mkString(", "))
    }
    val workflowDatasets = datasets.map(_.task.toString).toSet
    val actualVariableInputs = replaceableInputs.filter(id => workflowDatasets.contains(id) && workflowDatasetInputs.contains(id))
    val actualVariableOutputs = replaceableOutputs.filter(id => workflowDatasets.contains(id) && workflowDatasetOutputs.contains(id))
    AllReplaceableDatasets(actualVariableInputs, actualVariableOutputs)
  }

  /** Returns all replaceable input and output datasets that exist in the workflow or a nested workflows and were marked as replaceable dataset. */
  def markedReplaceableDatasets(project: Project)
                               (implicit userContext: UserContext): AllReplaceableDatasets = {
    val AllReplaceableDatasets(actualVariableInputs, actualVariableOutputs) = validateAndGetReplaceableDatasetsOfCurrentWorkflow()
    var result = AllReplaceableDatasets(
      dataSources = actualVariableInputs,
      sinks = actualVariableOutputs
    )
    for (nestedWorkflow <- nestedWorkflows(project)) {
      result = result ++ nestedWorkflow.markedReplaceableDatasets(project)
    }
    result
  }

  /**
    * Returns all legacy variable datasets and how they are used in the workflow.
    *
    * @param project
    * @return
    * @throws Exception if a variable dataset is used as input and output, which is not allowed.
    */
  def legacyVariableDatasets(project: Project)
                            (implicit userContext: UserContext): AllReplaceableDatasets = {
    val variableDatasetsUsedInOutput =
      for (datasetTask <- outputDatasets(project)
           if datasetTask.data.plugin.isInstanceOf[VariableDataset]) yield {
        datasetTask.id.toString
      }

    val variableDatasetsUsedInInput =
      for (datasetTask <- inputDatasets(project)
           if datasetTask.data.plugin.isInstanceOf[VariableDataset]) yield {
        datasetTask.id.toString
      }
    val bothInAndOut = variableDatasetsUsedInInput.toSet & variableDatasetsUsedInOutput.toSet
    if (bothInAndOut.nonEmpty) {
      throw new scala.Exception("Cannot use variable dataset as input AND output! Affected datasets: " + bothInAndOut.mkString(", "))
    }
    var result = AllReplaceableDatasets(variableDatasetsUsedInInput.distinct, variableDatasetsUsedInOutput.distinct)
    for (nestedWorkflow <- nestedWorkflows(project)) {
      result = result ++ nestedWorkflow.legacyVariableDatasets(project)
    }
    result
  }

  /** Returns all Dataset tasks that are used as input in the workflow */
  def inputDatasets(project: Project)
                   (implicit userContext: UserContext): Seq[ProjectTask[DatasetSpec[Dataset]]] = {
    for (datasetNodeId <- operators.flatMap(_.allInputs).distinct;
         dataset <- project.taskOption[DatasetSpec[Dataset]](nodeById(datasetNodeId).task)) yield {
      dataset
    }
  }

  private def nestedWorkflows(project: Project)
                             (implicit userContext: UserContext): Seq[Workflow] = {
    operators
      .flatMap(op => project.anyTaskOption(op.task))
      .filter(_.data.isInstanceOf[Workflow])
      .map(_.data.asInstanceOf[Workflow])
  }

  /** Legacy and marked replaceable datasets combined. This also adds replaceable datasets from nested workflows. */
  def allReplaceableDatasets(project: Project)
                            (implicit userContext: UserContext): AllReplaceableDatasets = {
    val legacy = legacyVariableDatasets(project)
    val marked = markedReplaceableDatasets(project)
    val all = legacy ++ marked
    val inAndOut = all.dataSources.intersect(all.sinks)
    if(inAndOut.nonEmpty) {
      val datasetLabels = inAndOut.map(id => project.task[GenericDatasetSpec](id).fullLabel).sorted
      throw new ValidationException("Following replaceable/variable datasets are used as input and output at " +
        "the same time in the workflow and nested workflows: " + datasetLabels.mkString(","))
    }
    all
  }

  /** Returns all Dataset tasks that are used as outputs in the workflow */
  def outputDatasets(project: Project)
                    (implicit userContext: UserContext): Seq[ProjectTask[DatasetSpec[Dataset]]] = {
    val configInputs = new mutable.HashMap[String, String]()
    for (reConfiguredDataset <- datasets.filter(_.configInputs.nonEmpty)) {
      configInputs.put(reConfiguredDataset.nodeId, reConfiguredDataset.configInputs.head)
    }
    val operatorsWithDataOutput: Set[Identifier] = operators
      .map(op => op.task).distinct
      .filter(taskId => project.anyTaskOption(taskId).map(_.outputPort.isDefined).getOrElse(false))
      .toSet
    // Filter out datasets that have no real data input
    val datasetNodesWithRealInputs = operators.flatMap(op => {
      if(!operatorsWithDataOutput.contains(op.task)) {
        // The operator node that goes into the dataset must have real data output
        Seq.empty
      } else {
        // and is not connected to the config port of the dataset
        op.outputs.filter(output => !configInputs.get(output).contains(op.nodeId))
      }
    }).distinct
    for (datasetNodeId <- datasetNodesWithRealInputs;
         dataset <- project.taskOption[DatasetSpec[Dataset]](nodeById(datasetNodeId).task)) yield {
      dataset
    }
  }

  /**
    * Returns all direct sub workflows.
    */
  def subWorkflows(project: Project)
                  (implicit userContext: UserContext): Seq[ProjectTask[Workflow]] = {
    for (operator <- operators;
         workflow <- project.taskOption[Workflow](operator.task)) yield {
      workflow
    }
  }

  /** Returns node ids of workflow nodes that have inputs (data or dependency) from other nodes */
  def inputWorkflowNodeIds(): Seq[String] = {
    val outputs = nodes.flatMap(_.outputs).distinct
    val nodesWithInputs = nodes.filter(n => n.allIncomingNodes.nonEmpty).map(_.nodeId)
    (outputs ++ nodesWithInputs).distinct
  }

  /** Returns node IDs of workflow nodes that have a dependency output. */
  def dependencyOutputNodes(): Seq[String] = {
    (datasets.value.flatMap(_.dependencyInputs) ++ operators.value.flatMap(_.dependencyInputs)).distinct
  }

  /** Returns node ids of workflow nodes that have data neither inputs nor outputs nor input/output dependencies */
  def singleWorkflowNodes(): Seq[String] = {
    val depOutNodes = dependencyOutputNodes().toSet
    nodes.filter(n => n.allIncomingNodes.isEmpty && n.outputs.isEmpty).map(_.nodeId).filter(!depOutNodes.contains(_))
  }

  /** Returns node ids of workflow nodes that have output connections (data or dependency) to other nodes */
  def outputWorkflowNodeIds(): Seq[String] = {
    val inputs = nodes.flatMap(_.allIncomingNodes).distinct
    val nodesWithOutputs = nodes.filter(_.outputs.nonEmpty).map(_.nodeId)
    (inputs ++ nodesWithOutputs).distinct
  }

  /**
    * At the moment, a workflow does not have any inputs.
    * It still declares a flexible number of inputs so those can be used to model dependencies.
    */
  override def inputPorts: InputPorts = FlexibleNumberOfInputs()

  /**
    * At the moment, a workflow does not have any output.
    * It still declares an output, so it can be used to model dependencies.
    */
  override def outputPort: Option[Port] = None

  /**
    * The tasks that this task reads from.
    */
  override def inputTasks: Set[Identifier] = nodes.filter(_.outputs.nonEmpty).map(_.task).toSet

  /**
    * The tasks that this task writes to.
    */
  override def outputTasks: Set[Identifier] = nodes.filter(_.inputs.exists(_.isDefined)).map(_.task).toSet

  /**
    * All tasks in this workflow.
    */
  override def referencedTasks: Set[Identifier] = nodes.map(_.task).toSet

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

  override def mainActivities: Seq[String] = Seq("ExecuteDefaultWorkflow")

  override def searchTags: Seq[String] = {
    var l = Vector.empty[String]
    if(replaceableInputs.nonEmpty) {
      l = l :+ "Replaceable input"
    }
    if(replaceableOutputs.nonEmpty) {
      l = l :+ "Replaceable output"
    }
    l
  }
}

/** Plugin parameter for the workflow operators. */
case class WorkflowOperatorsParameter(value: Seq[WorkflowOperator]) extends PluginObjectParameterNoSchema

object WorkflowOperatorsParameter {
  implicit def toWorkflowOperatorParameter(v: Seq[WorkflowOperator]): WorkflowOperatorsParameter = WorkflowOperatorsParameter(v)
  implicit def fromWorkflowOperatorParameter(v: WorkflowOperatorsParameter): Seq[WorkflowOperator] = v.value

  implicit object WorkflowOperatorsFormat extends XmlFormat[WorkflowOperatorsParameter] {
    override def read(node: Node)(implicit readContext: ReadContext): WorkflowOperatorsParameter = {
      for (op <- node \ "Operator") yield {
        WorkflowOperator.workflowOperatorXmlFormat.read(op)
      }
    }

    override def write(operators: WorkflowOperatorsParameter)(implicit writeContext: WriteContext[Node]): Node = {
      <WorkflowOperators>
        {for (op <- operators.value) yield {
        WorkflowOperator.workflowOperatorXmlFormat.write(op)
      }}
      </WorkflowOperators>
    }
  }
}

/** Plugin parameter for the workflow datasets. */
case class WorkflowDatasetsParameter(value: Seq[WorkflowDataset]) extends PluginObjectParameterNoSchema

object WorkflowDatasetsParameter {
  implicit def toWorkflowDatasetParameter(v: Seq[WorkflowDataset]): WorkflowDatasetsParameter = WorkflowDatasetsParameter(v)
  implicit def fromWorkflowDatasetParameter(v: WorkflowDatasetsParameter): Seq[WorkflowDataset] = v.value

  implicit object WorkflowDatasetsFormat extends XmlFormat[WorkflowDatasetsParameter] {
    override def read(node: Node)(implicit readContext: ReadContext): WorkflowDatasetsParameter = {
      for (op <- node \ "Dataset") yield {
        WorkflowDataset.workflowDatasetXmlFormat.read(op)
      }
    }

    override def write(operators: WorkflowDatasetsParameter)(implicit writeContext: WriteContext[Node]): Node = {
      <WorkflowDatasets>
        {for (op <- operators.value) yield {
        WorkflowDataset.workflowDatasetXmlFormat.write(op)
      }}
      </WorkflowDatasets>
    }
  }
}

/** Used e.g. for replaceable input/output lists. */
case class TaskIdentifierParameter(taskIds: Seq[String]) extends PluginObjectParameterNoSchema

object TaskIdentifierParameter {
  implicit def toTaskIdentifierParameter(v: Seq[String]): TaskIdentifierParameter = TaskIdentifierParameter(v)

  implicit def fromTaskIdentifierParameter(v: TaskIdentifierParameter): Seq[String] = v.taskIds

  implicit object TaskIdentifierParameterXmlFormat extends XmlFormat[TaskIdentifierParameter] {
    override def read(value: Node)(implicit readContext: ReadContext): TaskIdentifierParameter = {
      TaskIdentifierParameter(Workflow.taskIds(value.text))
    }

    override def write(value: TaskIdentifierParameter)(implicit writeContext: WriteContext[Node]): Node = {
      Text(value.taskIds.mkString(","))
    }
  }
}

/** All IDs of replaceable datasets in a workflow */
case class AllReplaceableDatasets(dataSources: Seq[String], sinks: Seq[String]) {
  def ++(other: AllReplaceableDatasets): AllReplaceableDatasets = {
    AllReplaceableDatasets(
      (dataSources ++ other.dataSources).distinct,
      (sinks ++ other.sinks).distinct
    )
  }
}

/** The workflow dependency graph */
case class WorkflowDependencyGraph(startNodes: Seq[WorkflowDependencyNode],
                                   endNodes: Seq[WorkflowDependencyNode])


object Workflow {

  /** Extract task IDs from a comma concatenated string. */
  def taskIds(concatenatedIdsString: String): Seq[String] = {
    if (concatenatedIdsString.isEmpty) {
      Seq.empty
    } else {
      concatenatedIdsString.split(",").toSeq
    }
  }

  implicit object WorkflowXmlFormat extends XmlFormat[Workflow] {

    override def tagNames: Set[String] = Set("Workflow")

    /**
      * Deserialize a value from XML.
      */
    override def read(xml: Node)(implicit readContext: ReadContext): Workflow = {
      val operators =
        for (op <- xml \ "Operator") yield {
          WorkflowOperator.workflowOperatorXmlFormat.read(op)
        }

      val datasets =
        for (ds <- xml \ "Dataset") yield {
          WorkflowDataset.workflowDatasetXmlFormat.read(ds)
        }

      val stickyNotes = (xml \ "UiAnnotations" \ "StickyNotes" \ "StickyNote").map(StickyNote.StickyNodeXmlFormat.read)
      val replaceableInputs = taskIds((xml \ "@replaceableInputs").text.trim)
      val replaceableOutputs = taskIds((xml \ "@replaceableOutputs").text.trim)
      new Workflow(operators, datasets, UiAnnotations(stickyNotes), replaceableInputs, replaceableOutputs)
    }

    /**
      * Serialize a value to XML.
      */
    override def write(workflow: Workflow)(implicit writeContext: WriteContext[Node]): Node = {
      import workflow._
      <Workflow replaceableInputs={workflow.replaceableInputs.mkString(",")} replaceableOutputs={workflow.replaceableOutputs.mkString(",")}>
        {for (op <- operators) yield {
          WorkflowOperator.workflowOperatorXmlFormat.write(op)
      }}{for (ds <- datasets) yield {
        WorkflowDataset.workflowDatasetXmlFormat.write(ds)
      }}
        {UiAnnotations.UiAnnotationsXmlFormat.write(workflow.uiAnnotations)}
      </Workflow>
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

  /**
    * Returns all nodes that directly precede this node.
    */
  def precedingNodes: Set[WorkflowDependencyNode] = _precedingNodes

  /**
    * Returns all nodes that directly or indirectly precede this node.
    */
  def precedingNodesRecursively: Set[WorkflowDependencyNode] = {
    precedingNodes ++ precedingNodes.flatMap(_.precedingNodesRecursively)
  }

  /** The direct input nodes as [[WorkflowDependencyNode]] */
  def inputNodes: Seq[WorkflowDependencyNode] = {
    for (
      input <- workflowNode.inputs.flatten;
      pNode <- precedingNodes.filter(_.nodeId == input)) yield {
      pNode
    }
  }

  /** The direct dependency input nodes as [[WorkflowDependencyNode]] */
  def dependencyInputNodes: Seq[WorkflowDependencyNode] = {
    val nodeSet = workflowNode.dependencyInputs.toSet
    precedingNodes.filter(n => nodeSet.contains(n.nodeId)).toSeq
  }

  /** The config input nodes as [[WorkflowDependencyNode]] */
  def configInputNodes: Seq[WorkflowDependencyNode] = {
    for (
      configInput <- workflowNode.configInputs;
      pNode <- precedingNodes.filter(_.nodeId == configInput)) yield {
      pNode
    }
  }
}
