package org.silkframework.workspace.activity.workflow

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.dataset.Dataset
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}

/**
  * Created on 7/21/16.
  */
class WorkflowTest extends FlatSpec with MockitoSugar with MustMatchers {
  behavior of "Workflow"

  it should "support sorting its workflow operators topologically" in {
    val project = mock[Project]
    val workflow = testWorkflow
    for (dataset <- workflow.datasets) {
      val id = Identifier(dataset.nodeId)
      val datasetTask = mock[ProjectTask[Dataset]]
      when(datasetTask.id).thenReturn(id)
      when(project.taskOption[Dataset](dataset.task)).thenReturn(Some(datasetTask))
    }
    val sortedWorkflowNodes = workflow.topologicalSortedNodes.map(_.nodeId)
    sortedWorkflowNodes mustBe Seq("dsA1", "dsA2", "transform1", "transform2", "dsB1", "dsB2", "links", "output", "linking", "generateOutput")
  }

  it should "detect circular workflows" in {
    intercept[RuntimeException] {
      circularWorkflow.topologicalSortedNodes
    }
    intercept[RuntimeException] {
      circularWorkflow.workflowDependencyGraph
    }
  }

  it should "generate a DAG of the node dependencies" in {
    val dag = testWorkflow.workflowDependencyGraph
    dag mustBe testWorkflow.WorkflowDependencyGraph(
      startNodes = Set(
        testWorkflow.WorkflowDependencyNode(WorkflowDataset(List(), "dsA1", List(), (0, 0), "dsA1")),
        testWorkflow.WorkflowDependencyNode(WorkflowDataset(List(), "dsA2", List(), (0, 0), "dsA2"))),
      endNodes = Set(
        testWorkflow.WorkflowDependencyNode(WorkflowDataset(List(), "output", List(), (0, 0), "output"))))
    val dsA1 = dag.startNodes.filter(_.workflowNode.nodeId == "dsA1").head
    intercept[IllegalStateException] {
      dsA1.addFollowingNode(null)
    }
    var current = dsA1
    for (nextLabel <- Seq("transform1", "dsB1", "linking", "links", "generateOutput", "output")) {
      current.followingNodes.size mustBe 1
      val next = current.followingNodes.head
      next.nodeId mustBe nextLabel
      next.precedingNodes must contain(current)
      if (!next.workflowNode.isInstanceOf[WorkflowDataset]) {
        next.inputNodes must contain(current)
      }
      current = next
    }
    current mustBe dag.endNodes.head
  }

  it should "sort correctly for a workflow ending in an operator" in {
    val sortedNodes = testWorkflowEndingInOperator.topologicalSortedNodes
    sortedNodes.map(_.nodeId) mustBe Seq("dsA", "dsB", "transform")
  }

  it should "build the DAG correctly for a workflow ending in an operator" in {
    val dag = testWorkflowEndingInOperator.workflowDependencyGraph
    dag.startNodes.map(_.nodeId) mustBe Set("dsA", "dsB")
    dag.endNodes.map(_.nodeId) mustBe Set("transform")
  }

  it should "sort correctly for a workflow with disjunct data flows and multiple output nodes" in {
    val sortedNodes = testWorkflowWithMultipleEndNodesAndDisjunctDataFlows.topologicalSortedNodes
    sortedNodes.map(_.nodeId) mustBe Seq("dsA", "dsB", "dsC", "transform", "op1", "op2")
  }

  it should "build the DAG correctly for a workflow with disjunct data flows and multiple output nodes" in {
    val dag = testWorkflowWithMultipleEndNodesAndDisjunctDataFlows.workflowDependencyGraph
    dag.startNodes.map(_.nodeId) mustBe Set("dsA", "dsB", "dsC")
    dag.endNodes.map(_.nodeId) mustBe Set("transform", "op1", "op2")
  }

  val testWorkflow: Workflow = {
    Workflow(
      Identifier("workflow"),
      operators = Seq(
        operator(task = "transform1", inputs = Seq("dsA1"), outputs = Seq("dsB1"), "transform1"),
        operator(task = "transform2", inputs = Seq("dsA2"), outputs = Seq("dsB1"), "transform2"),
        operator(task = "linking", inputs = Seq("dsB1", "dsB1"), outputs = Seq("links"), "linking"),
        operator(task = "generateOutput", inputs = Seq("links"), outputs = Seq("output"), "generateOutput")
      ),
      datasets = Seq(
        dataset("dsA1", "dsA1"),
        dataset("dsA2", "dsA2"),
        dataset("dsB", "dsB1"),
        dataset("dsB", "dsB2"),
        dataset("links", "links"),
        dataset("output", "output")
      ))
  }

  val testWorkflowEndingInOperator: Workflow = {
    Workflow(
      Identifier("workflow"),
      operators = Seq(
        operator(task = "transform", inputs = Seq("dsA", "dsB"), outputs = Seq(), "transform")
      ),
      datasets = Seq(
        dataset("dsA", "dsA"),
        dataset("dsB", "dsB")
      ))
  }

  val testWorkflowWithMultipleEndNodesAndDisjunctDataFlows: Workflow = {
    Workflow(
      Identifier("workflow"),
      operators = Seq(
        operator(task = "transform", inputs = Seq("dsA", "dsB"), outputs = Seq(), "transform"),
        operator(task = "op1", inputs = Seq("dsC"), outputs = Seq(), "op1"),
        operator(task = "op2", inputs = Seq("dsC"), outputs = Seq(), "op2")
      ),
      datasets = Seq(
        dataset("dsA", "dsA"),
        dataset("dsB", "dsB"),
        dataset("dsC", "dsC")
      ))
  }

  val circularWorkflow: Workflow = {
    Workflow(
      Identifier("circularWorkflow"),
      operators = Seq(
        operator(task = "transform1", inputs = Seq("transform2"), outputs = Seq("transform2"), "transform1"),
        operator(task = "transform2", inputs = Seq("transform1"), outputs = Seq("transform1"), "transform2")
      ),
      datasets = Seq()
    )
  }

  def operator(task: String, inputs: Seq[String], outputs: Seq[String], nodeId: String): WorkflowOperator = {
    WorkflowOperator(inputs = inputs, task = task, outputs = outputs, Seq(), (0, 0), nodeId)
  }

  def dataset(task: String, nodeId: String): WorkflowDataset = {
    WorkflowDataset(Seq(), task, Seq(), (0, 0), nodeId)
  }
}
