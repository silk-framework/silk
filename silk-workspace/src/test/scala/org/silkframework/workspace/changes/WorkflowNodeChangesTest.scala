package org.silkframework.workspace.changes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowDataset, WorkflowOperator}

/** The typed workflow graph changes: what they describe, how they apply, conflict and revert. */
class WorkflowNodeChangesTest extends AnyFlatSpec with Matchers {

  behavior of "workflow node changes"

  private def dataset(nodeId: String, inputs: Seq[Option[String]] = Seq.empty, outputs: Seq[String] = Seq.empty): WorkflowDataset = {
    WorkflowDataset(inputs = inputs, task = nodeId, outputs = outputs, position = (0, 0), nodeId = nodeId,
      configInputs = Seq.empty, dependencyInputs = Seq.empty)
  }

  private def operator(nodeId: String, inputs: Seq[Option[String]] = Seq.empty, outputs: Seq[String] = Seq.empty,
                       errorOutputs: Seq[String] = Seq.empty, dependencyInputs: Seq[String] = Seq.empty): WorkflowOperator = {
    WorkflowOperator(inputs = inputs, task = nodeId, outputs = outputs, errorOutputs = errorOutputs,
      position = (0, 0), nodeId = nodeId, configInputs = Seq.empty, dependencyInputs = dependencyInputs)
  }

  private def flowTask(workflow: Workflow): PlainTask[Workflow] = PlainTask("flow", workflow)

  it should "add a node, refuse a taken id and revert the addition by removing the node" in {
    val flow = Workflow(datasets = Seq(dataset("csv")))
    val add = AddWorkflowNode("flow", operator("transform"))
    add.describe shouldBe "Added operator node 'transform' to workflow 'flow'"
    val added = add(flow)
    added.nodes.map(_.nodeId) shouldBe Seq("transform", "csv")
    a[ChangeConflictException] should be thrownBy add(added)

    val remove = add.inverse.get
    remove.describe shouldBe "Removed operator node 'transform' from workflow 'flow'"
    remove(added) shouldBe flow

    // A node whose id does not name its task shows the task
    AddWorkflowNode("flow", dataset("csv_2").copy(task = "csv")).describe shouldBe
      "Added dataset node 'csv_2' (task 'csv') to workflow 'flow'"
  }

  it should "resolve a connect request to the first vacant input port, the end, or the given port" in {
    val flow = Workflow(operators = Seq(operator("t", inputs = Seq(None, Some("a"))), operator("a", outputs = Seq("t"))),
      datasets = Seq(dataset("csv")))
    val task = flowTask(flow)

    val connect = ConnectWorkflowNodes.data(task, "csv", "t", targetInputIndex = None).get
    connect.describe shouldBe "Connected 'csv' to 't' in workflow 'flow'"
    connect.edge shouldBe WorkflowEdge.Data(Seq(0))
    val connected = connect(flow)
    connected.nodeById("t").inputs shouldBe Seq(Some("csv"), Some("a"))
    connected.nodeById("csv").outputs shouldBe Seq("t")

    // No vacant port left: the next connection is appended; a given port is padded to
    ConnectWorkflowNodes.data(flowTask(connected), "a", "csv", None).get.edge shouldBe WorkflowEdge.Data(Seq(0))
    ConnectWorkflowNodes.data(flowTask(connected), "a", "csv", Some(2)).get.edge shouldBe WorkflowEdge.Data(Seq(2))

    // Idempotent when the edge exists; occupied and negative ports are the user's error
    ConnectWorkflowNodes.data(flowTask(connected), "csv", "t", None) shouldBe None
    a[BadUserInputException] should be thrownBy ConnectWorkflowNodes.data(flowTask(connected), "csv", "t", Some(1))
    a[BadUserInputException] should be thrownBy ConnectWorkflowNodes.data(task, "csv", "t", Some(-1))
    a[NotFoundException] should be thrownBy ConnectWorkflowNodes.data(task, "missing", "t", None)

    // Applying twice conflicts, e.g. after a concurrent identical connect
    a[ChangeConflictException] should be thrownBy connect(connected)
  }

  it should "record a dependency edge on the target and revert it" in {
    val flow = Workflow(operators = Seq(operator("a"), operator("b")))
    val connect = ConnectWorkflowNodes.dependency(flowTask(flow), "a", "b").get
    connect.describe shouldBe "Connected 'a' to 'b' (dependency) in workflow 'flow'"
    val connected = connect(flow)
    connected.nodeById("b").dependencyInputs shouldBe Seq("a")
    ConnectWorkflowNodes.dependency(flowTask(connected), "a", "b") shouldBe None
    connect.inverse.get(connected) shouldBe flow
  }

  it should "disconnect what connects two nodes and reconnect it at the same port by reverting" in {
    val flow = Workflow(
      operators = Seq(operator("t", inputs = Seq(Some("csv")), dependencyInputs = Seq("csv"))),
      datasets = Seq(dataset("csv", outputs = Seq("t"))))
    val changes = DisconnectWorkflowNodes.of(flowTask(flow), "csv", "t", data = true, dependency = true)
    changes.map(_.describe) shouldBe Seq(
      "Disconnected 'csv' from 't' in workflow 'flow'",
      "Disconnected 'csv' from 't' (dependency) in workflow 'flow'")
    changes.head.edge shouldBe WorkflowEdge.Data(Seq(0))

    val cleaned = changes.foldLeft(flow)((current, change) => change(current))
    cleaned.nodeById("t").inputs shouldBe Seq.empty
    cleaned.nodeById("t").dependencyInputs shouldBe Seq.empty
    cleaned.nodeById("csv").outputs shouldBe Seq.empty
    // A vanished edge is not disconnected again
    DisconnectWorkflowNodes.of(flowTask(cleaned), "csv", "t", data = true, dependency = true) shouldBe empty

    val restored = changes.reverse.foldLeft(cleaned)((current, change) => change.inverse.get(current))
    restored shouldBe flow
  }

  it should "capture a node removal as its disconnections followed by the removal of the bare node" in {
    val flow = Workflow(
      operators = Seq(
        operator("transform", inputs = Seq(Some("csv")), outputs = Seq("out"), errorOutputs = Seq("err"), dependencyInputs = Seq("dep")),
        operator("dep")),
      datasets = Seq(
        dataset("csv", outputs = Seq("transform")),
        dataset("out", inputs = Seq(Some("transform"))),
        dataset("err")))
    val changes = RemoveWorkflowNode.of(flowTask(flow), "transform")
    changes.map(_.describe) shouldBe Seq(
      "Disconnected 'dep' from 'transform' (dependency) in workflow 'flow'",
      "Disconnected 'csv' from 'transform' in workflow 'flow'",
      "Disconnected 'transform' from 'out' in workflow 'flow'",
      "Disconnected 'transform' from 'err' (error output) in workflow 'flow'",
      "Removed operator node 'transform' from workflow 'flow'")

    val removed = changes.foldLeft(flow)((current, change) => change(current))
    removed.nodes.map(_.nodeId) shouldBe Seq("dep", "csv", "out", "err")
    removed.nodeById("csv").outputs shouldBe Seq.empty
    removed.nodeById("out").inputs shouldBe Seq.empty

    // Reverting newest-first restores the node, then each of its edges
    val restored = changes.reverse.foldLeft(removed)((current, change) => change.inverse.get(current))
    restored.nodes.toSet shouldBe flow.nodes.toSet

    // The bare removal refuses while the node is still connected (it differs from the disconnected capture)
    a[ChangeConflictException] should be thrownBy changes.last(flow)
    a[NotFoundException] should be thrownBy RemoveWorkflowNode.of(flowTask(flow), "missing")
  }

  it should "remove and restore a node with a self-loop, which travels with the node" in {
    val flow = Workflow(operators = Seq(operator("loop", inputs = Seq(Some("loop")), outputs = Seq("loop"))))
    val changes = RemoveWorkflowNode.of(flowTask(flow), "loop")
    changes.map(_.describe) shouldBe Seq("Removed operator node 'loop' from workflow 'flow'")
    val removed = changes.head(flow)
    removed.nodes shouldBe empty
    changes.head.inverse.get(removed) shouldBe flow

    // Edge changes with equal ids are refused rather than half-applied
    an[IllegalArgumentException] should be thrownBy
      ConnectWorkflowNodes("flow", "loop", "loop", WorkflowEdge.Dependency)(flow)
  }

  it should "drop and restore the replaceable marking with the last node of a dataset task" in {
    val flow = Workflow(datasets = Seq(dataset("csv")), replaceableInputs = Seq("csv"), replaceableOutputs = Seq("csv"))
    val remove = RemoveWorkflowNode.of(flowTask(flow), "csv").last.asInstanceOf[RemoveWorkflowNode]
    remove.replaceableInput shouldBe true
    remove.replaceableOutput shouldBe true

    val removed = remove(flow)
    removed.datasets.value shouldBe empty
    removed.replaceableInputs.taskIds shouldBe empty
    remove.inverse.get(removed) shouldBe flow
  }
}
