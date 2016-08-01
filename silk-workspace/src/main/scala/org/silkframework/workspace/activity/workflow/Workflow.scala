package org.silkframework.workspace.activity.workflow

import org.silkframework.config.TaskSpec
import org.silkframework.dataset.{Dataset, VariableDataset}
import org.silkframework.entity.EntitySchema
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}

import scala.xml.Node

/**
  * A workflow is a DAG, whose nodes are either datasets or operators and specifies the data flow between them.
 *
  * @param id of the workflow
  * @param operators Operators, e.g. transformations and link specs.
  * @param datasets
  */
case class Workflow(id: Identifier, operators: Seq[WorkflowOperator], datasets: Seq[WorkflowDataset]) extends TaskSpec {

  lazy val nodes: Seq[WorkflowNode] = operators ++ datasets

  def nodeById(nodeId: String) = {
    nodes.find(_.nodeId == nodeId)
        .getOrElse(throw new NoSuchElementException(s"Cannot find node $nodeId in the workflow."))
  }

  def toXML = {
    <Workflow id={id.toString}>
      {for (op <- operators) yield {
        <Operator
        posX={op.position._1.toString}
        posY={op.position._2.toString}
        task={op.task}
        inputs={op.inputs.mkString(",")}
        outputs={op.outputs.mkString(",")}
        errorOutputs={op.errorOutputs.mkString(",")}
        id={op.nodeId}/>
    }}{for (ds <- datasets) yield {
        <Dataset
        posX={ds.position._1.toString}
        posY={ds.position._2.toString}
        task={ds.task}
        inputs={ds.inputs.mkString(",")}
        outputs={ds.outputs.mkString(",")}
        id={ds.nodeId}/>
    }}
    </Workflow>
  }

  /**
    * A topologically sorted sequence of [[WorkflowOperator]] used in this workflow.
    */
  lazy val topologicalSortedNodes: IndexedSeq[WorkflowNode] = {
    val inputs = inputWorkflowNodeIds()
    val outputs = outputWorkflowNodeIds()
    val pureOutputNodes = outputs.toSet -- inputs
    var done = pureOutputNodes
    var sortedOperators = Vector.empty[WorkflowNode]
    val (start, rest) = nodes.partition(node => pureOutputNodes.contains(node.nodeId))
    sortedOperators ++= start
    var operatorsToSort = rest
    while (operatorsToSort.size > 0) {
      val (satisfied, unsatisfied) = operatorsToSort.partition(op => op.inputs.forall(done))
      if (satisfied.size == 0) {
        throw new RuntimeException("Cannot topologically sort operators in workflow " + id.toString + "!")
      }
      sortedOperators ++= satisfied
      done ++= satisfied.map(_.nodeId)
      operatorsToSort = unsatisfied
    }
    sortedOperators
  }

  /**
    * Returns a dependency graph that can be traversed from the start or end nodes and consists of
    * double linked nodes.
    */
  lazy val workflowDependencyGraph: WorkflowDependencyGraph = {
    // Test if this graph can be topologically sorted
    topologicalSortedNodes
    val inputs = inputWorkflowNodeIds()
    val outputs = outputWorkflowNodeIds()
    val startNodes = outputs.toSet -- inputs
    val endNodes = inputs.toSet -- outputs
    val workflowNodeMap: Map[String, WorkflowDependencyNode] = constructNodeMap
    val startDependencyNodes = startNodes.map(workflowNodeMap)
    val endDependencyNodes = endNodes.map(workflowNodeMap)
    WorkflowDependencyGraph(startDependencyNodes, endDependencyNodes)
  }

  private def constructNodeMap: Map[String, WorkflowDependencyNode] = {
    val workflowNodeMap = nodes.map(n => (n.nodeId, WorkflowDependencyNode(n))).toMap
    for (node <- nodes) {
      val depNode = workflowNodeMap(node.nodeId)
      for (inputNode <- node.inputs) {
        val precedingNode = workflowNodeMap.getOrElse(inputNode,
          throw new scala.RuntimeException("Unsatisfiable input dependency in workflow " + id.toString + "! Dependency: " + inputNode))
        depNode.addPrecedingNode(precedingNode)
        precedingNode.addFollowingNode(depNode)
      }
      for (outputNode <- node.outputs) {
        val followingNode = workflowNodeMap.getOrElse(outputNode,
          throw new scala.RuntimeException("Unsatisfiable output dependency in workflow " + id.toString + "! Dependency: " + outputNode))
        depNode.addFollowingNode(followingNode)
        followingNode.addPrecedingNode(depNode)
      }
    }
    // Make immutable
    workflowNodeMap.map(_._2.setToImmutable())
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
    if (bothInAndOut.size > 0) {
      throw new scala.Exception("Cannot use variable dataset as input AND output! Affected datasets: " + bothInAndOut.mkString(", "))
    }
    AllVariableDatasets(variableDatasetsUsedInInput, variableDatasetsUsedInOutput)
  }

  def inputDatasets(project: Project): Seq[ProjectTask[Dataset]] = {
    for (datasetNodeId <- operators.flatMap(_.inputs).distinct;
         dataset <- project.taskOption[Dataset](nodeById(datasetNodeId).task)) yield {
      dataset
    }
  }

  def outputDatasets(project: Project): Seq[ProjectTask[Dataset]] = {
    for (datasetNodeId <- operators.flatMap(_.outputs).distinct;
         dataset <- project.taskOption[Dataset](nodeById(datasetNodeId).task)) yield {
      dataset
    }
  }

  /** Returns node ids of workflow nodes that have inputs from other nodes */
  def inputWorkflowNodeIds(): Seq[String] = {
    nodes.flatMap(_.outputs).distinct
  }

  /** Returns node ids of workflow nodes that output data into other nodes */
  def outputWorkflowNodeIds(): Seq[String] = {
    nodes.flatMap(_.inputs).distinct
  }

  case class AllVariableDatasets(dataSources: Seq[String], sinks: Seq[String])

  case class WorkflowDependencyGraph(startNodes: Iterable[WorkflowDependencyNode],
                                     endNodes: Iterable[WorkflowDependencyNode])

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

    def nodeId = workflowNode.nodeId

    def isMutable = mutableNode

    def addPrecedingNode(node: WorkflowDependencyNode): Unit = {
      if(isMutable) {
        _precedingNodes += node
      } else {
        throw new IllegalStateException("Cannot add node to preceding nodes! This node is set to immutable!")
      }
    }

    def addFollowingNode(node: WorkflowDependencyNode): Unit = {
      if(isMutable) {
        _followingNodes += node
      } else {
        throw new IllegalStateException("Cannot add node to following nodes! This node is set to immutable!")
      }
    }

    def followingNodes = _followingNodes

    def precedingNodes = _precedingNodes

    def inputNodes: Seq[WorkflowDependencyNode] = {
      for(
        input <- workflowNode.inputs;
        pNode <- precedingNodes.filter(_.nodeId == input)) yield {
        pNode
      }
    }
  }

  /**
    * The schemata of the input data for this task.
    * A separate entity schema is returned for each input.
    */
override def inputSchemata: Seq[EntitySchema] = Seq()

  /**
    * The schema of the output data.
    * Returns None, if the schema is unknown or if no output is written by this task.
    */
  override def outputSchemaOpt: Option[EntitySchema] = None
}

object Workflow {

  def fromXML(xml: Node) = {
    val id = (xml \ "@id").text

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
          position = ((op \ "@posX").text.toInt, (op \ "@posY").text.toInt),
          nodeId = {
            val node = ((op \ "@id"))
            if (node.isEmpty) {
              task
            } else {
              node.text
            }
          }
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
          position = ((ds \ "@posX").text.toInt, (ds \ "@posY").text.toInt),
          nodeId = {
            val node = ((ds \ "@id"))
            if (node.isEmpty) {
              task
            } else {
              node.text
            }
          }
        )
      }

    new Workflow(if (id.nonEmpty) Identifier(id) else Identifier.random, operators, datasets)
  }
}