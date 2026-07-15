package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.CustomTask
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.operations.ClearDatasetOperator
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.rule.{DatasetSelection, TransformSpec}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.workspace.activity.workflow.WorkflowTest.{dataset, operator}
import org.silkframework.workspace.resources.InMemoryResourceRepository
import org.silkframework.workspace.{InMemoryWorkspaceProvider, ProjectConfig, Workspace}

class ClearDatasetOrderingCheckTest extends AnyFlatSpec with Matchers with TestUserContextTrait {
  behavior of "ClearDatasetOrderingCheck"

  private val sourceDs = "sourceDs"
  private val targetDs = "targetDs"
  private val transform = "transform"
  private val clearOp = "clearOp"
  private val writeNode = "aWrite"
  private val clearNode = "zClear"

  private lazy val project = {
    val workspace = new Workspace(new InMemoryWorkspaceProvider(), InMemoryResourceRepository())
    val project = workspace.createProject(ProjectConfig("clearOrderingProject"))
    for(datasetId <- Seq(sourceDs, targetDs)) {
      project.addTask[GenericDatasetSpec](datasetId, DatasetSpec(CsvDataset(project.resources.get("file.csv"))))
    }
    project.addTask[TransformSpec](transform, TransformSpec(DatasetSelection(sourceDs)))
    project.addTask[CustomTask](clearOp, ClearDatasetOperator())
    project
  }

  /** Source -> transform -> write node of the target dataset; a clear operator feeding a second node of it. */
  private def workflow(writerNode: WorkflowDataset, clearDatasetNode: WorkflowDataset): Workflow = {
    Workflow(
      operators = Seq(
        operator(task = transform, inputs = Seq(sourceDs), outputs = Seq(writerNode.nodeId), transform),
        operator(task = clearOp, inputs = Seq(), outputs = Seq(clearDatasetNode.nodeId), clearOp)
      ),
      datasets = Seq(
        dataset(sourceDs, sourceDs, outputs = Seq(transform)),
        writerNode,
        clearDatasetNode
      ))
  }

  it should "report a clear node and a writer of the same dataset with no order between them" in {
    val pairs = ClearDatasetOrderingCheck.unorderedPairs(workflow(
      dataset(targetDs, writeNode, inputs = Seq(transform)),
      dataset(targetDs, clearNode, inputs = Seq(clearOp))), project)
    pairs.map(p => (p.datasetTask.toString, p.clearNodeId, p.writerNodeId)) mustBe Seq((targetDs, clearNode, writeNode))
    pairs.head.message must include (clearNode)
    pairs.head.message must include (writeNode)
  }

  it should "not report a pair when a dependency path orders the writer after the clear node" in {
    ClearDatasetOrderingCheck.unorderedPairs(workflow(
      dataset(targetDs, writeNode, inputs = Seq(transform), dependencyInputs = Seq(clearNode)),
      dataset(targetDs, clearNode, inputs = Seq(clearOp))), project) mustBe empty
  }

  it should "not report a pair when a dependency path orders the clear node after the writer" in {
    ClearDatasetOrderingCheck.unorderedPairs(workflow(
      dataset(targetDs, writeNode, inputs = Seq(transform)),
      dataset(targetDs, clearNode, inputs = Seq(clearOp), dependencyInputs = Seq(writeNode))), project) mustBe empty
  }

  it should "not report anything when clear and write happen on the same node" in {
    val singleNode = Workflow(
      operators = Seq(
        operator(task = transform, inputs = Seq(sourceDs), outputs = Seq(targetDs), transform),
        operator(task = clearOp, inputs = Seq(), outputs = Seq(targetDs), clearOp)
      ),
      datasets = Seq(
        dataset(sourceDs, sourceDs, outputs = Seq(transform)),
        dataset(targetDs, targetDs, inputs = Seq(clearOp, transform))
      ))
    ClearDatasetOrderingCheck.unorderedPairs(singleNode, project) mustBe empty
  }

  it should "not report a pair when the clear node has an explicit output priority" in {
    ClearDatasetOrderingCheck.unorderedPairs(workflow(
      dataset(targetDs, writeNode, inputs = Seq(transform)),
      dataset(targetDs, clearNode, inputs = Seq(clearOp), outputPriority = Some(1.0))), project) mustBe empty
  }

  it should "not report a pair when the writer node has an explicit output priority" in {
    // A defined priority runs before an undefined one, so the order is user-defined.
    ClearDatasetOrderingCheck.unorderedPairs(workflow(
      dataset(targetDs, writeNode, inputs = Seq(transform), outputPriority = Some(1.0)),
      dataset(targetDs, clearNode, inputs = Seq(clearOp))), project) mustBe empty
  }

  it should "not report a pair when clear and writer node have distinct output priorities" in {
    ClearDatasetOrderingCheck.unorderedPairs(workflow(
      dataset(targetDs, writeNode, inputs = Seq(transform), outputPriority = Some(2.0)),
      dataset(targetDs, clearNode, inputs = Seq(clearOp), outputPriority = Some(1.0))), project) mustBe empty
  }

  it should "report a pair when clear and writer node have equal output priorities" in {
    // Equal priorities leave the relative order unspecified.
    ClearDatasetOrderingCheck.unorderedPairs(workflow(
      dataset(targetDs, writeNode, inputs = Seq(transform), outputPriority = Some(1.0)),
      dataset(targetDs, clearNode, inputs = Seq(clearOp), outputPriority = Some(1.0))), project)
      .map(_.writerNodeId) mustBe Seq(writeNode)
  }

  it should "report a pair when the clear node has a priority but is not an end node" in {
    // The engine ignores priorities of non-end nodes, so the order remains undefined.
    val downstream = "downstream"
    val base = workflow(
      dataset(targetDs, writeNode, inputs = Seq(transform)),
      dataset(targetDs, clearNode, inputs = Seq(clearOp), outputs = Seq(downstream), outputPriority = Some(1.0)))
    val clearWithFollower = base.copy(
      operators = base.operators :+ operator(task = transform, inputs = Seq(clearNode), outputs = Seq(), downstream))
    ClearDatasetOrderingCheck.unorderedPairs(clearWithFollower, project).map(_.writerNodeId) mustBe Seq(writeNode)
  }

  it should "not treat a node with only config inputs as a writer" in {
    val configOnly = Workflow(
      operators = Seq(
        operator(task = transform, inputs = Seq(sourceDs), outputs = Seq(writeNode), transform),
        operator(task = clearOp, inputs = Seq(), outputs = Seq(clearNode), clearOp)
      ),
      datasets = Seq(
        dataset(sourceDs, sourceDs, outputs = Seq(transform)),
        dataset(targetDs, writeNode, configInputs = Seq(transform)),
        dataset(targetDs, clearNode, inputs = Seq(clearOp))
      ))
    ClearDatasetOrderingCheck.unorderedPairs(configOnly, project) mustBe empty
  }

  it should "report all unordered writers of a cleared dataset deterministically" in {
    val secondWriter = "bWrite"
    val base = workflow(
      dataset(targetDs, writeNode, inputs = Seq(transform)),
      dataset(targetDs, clearNode, inputs = Seq(clearOp)))
    val twoWriters = base.copy(
      operators = base.operators.map(op => if(op.nodeId == transform) op.copy(outputs = Seq(writeNode, secondWriter)) else op),
      datasets = base.datasets :+ dataset(targetDs, secondWriter, inputs = Seq(transform))
    )
    ClearDatasetOrderingCheck.unorderedPairs(twoWriters, project).map(_.writerNodeId) mustBe Seq(writeNode, secondWriter)
  }

  it should "return nothing for workflows without a clear operator, even invalid ones" in {
    // The fast exit must not force the (cyclic) dependency graph.
    ClearDatasetOrderingCheck.unorderedPairs(WorkflowTest.circularWorkflow, project) mustBe empty
  }
}
