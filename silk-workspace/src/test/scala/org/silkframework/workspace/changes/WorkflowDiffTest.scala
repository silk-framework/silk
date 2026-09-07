package org.silkframework.workspace.changes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.rule.NodePosition
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowDataset, WorkflowOperator}
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}

/** What a whole-workflow update lists as details: the nodes and edges that differ, in the words of the typed node changes. */
class WorkflowDiffTest extends AnyFlatSpec with Matchers {

  behavior of "workflow update details"

  private def dataset(nodeId: String, inputs: Seq[Option[String]] = Seq.empty, outputs: Seq[String] = Seq.empty,
                      position: (Int, Int) = (0, 0)): WorkflowDataset = {
    WorkflowDataset(inputs = inputs, task = nodeId, outputs = outputs, position = position, nodeId = nodeId,
      configInputs = Seq.empty, dependencyInputs = Seq.empty)
  }

  private def operator(nodeId: String, inputs: Seq[Option[String]] = Seq.empty, outputs: Seq[String] = Seq.empty,
                       configInputs: Seq[String] = Seq.empty, dependencyInputs: Seq[String] = Seq.empty): WorkflowOperator = {
    WorkflowOperator(inputs = inputs, task = nodeId, outputs = outputs, errorOutputs = Seq.empty,
      position = (0, 0), nodeId = nodeId, configInputs = configInputs, dependencyInputs = dependencyInputs)
  }

  private def details(before: Workflow, after: Workflow): Seq[String] = {
    ReplaceTask(PlainTask("flow", before), PlainTask("flow", after)).details.map(_.describe)
  }

  private val csv = Workflow(datasets = Seq(dataset("csv")))
  private val connected = Workflow(operators = Seq(operator("transform", inputs = Seq(Some("csv")))),
    datasets = Seq(dataset("csv", outputs = Seq("transform"))))

  it should "list the nodes and edges that were added or removed" in {
    details(csv, csv) shouldBe empty
    details(csv, connected) shouldBe Seq("Added operator node 'transform'", "Connected 'csv' to 'transform'")
    details(connected, csv) shouldBe Seq("Removed operator node 'transform'", "Disconnected 'csv' from 'transform'")
    ReplaceTask(PlainTask("flow", csv), PlainTask("flow", connected)).describe shouldBe
      "Updated workflow 'flow': Added operator node 'transform', Connected 'csv' to 'transform'"
    // A new workflow is described the same way, against the empty one
    AddTask(PlainTask("flow", connected)).describe shouldBe
      "Added workflow 'flow': Added operator node 'transform', Added dataset node 'csv', Connected 'csv' to 'transform'"
  }

  it should "name the kind of an edge and a node's changed task" in {
    val flow = Workflow(operators = Seq(operator("a"), operator("b")))
    val wired = Workflow(operators = Seq(operator("a"), operator("b", configInputs = Seq("a"), dependencyInputs = Seq("a"))))
    details(flow, wired) shouldBe Seq("Connected 'a' to 'b' (dependency)", "Connected 'a' to 'b' (config)")
    val retasked = Workflow(operators = Seq(operator("a"), operator("b").copy(task = "c")))
    details(flow, retasked) shouldBe Seq("Task of node 'b' 'b' → 'c'")
  }

  it should "report a pure move or note as an editor change and the replaceable datasets by their ids" in {
    val moved = Workflow(datasets = Seq(dataset("csv", position = (10, 20))))
    details(csv, moved) shouldBe Seq("Editor layout changed")
    val annotated = csv.copy(uiAnnotations = UiAnnotations(stickyNotes = Seq(StickyNote("note", "text", "#fff", NodePosition(0, 0)))))
    details(csv, annotated) shouldBe Seq("Editor layout changed")
    // A move next to a real change is not worth a line
    details(moved, connected) shouldBe Seq("Added operator node 'transform'", "Connected 'csv' to 'transform'")
    details(csv, csv.copy(replaceableInputs = Seq("csv"))) shouldBe Seq("Replaceable input datasets '' → 'csv'")
  }
}
