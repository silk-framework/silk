package org.silkframework.workspace.activity.workflow

import org.mockito.Mockito._
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.runtime.activity.{TestUserContextTrait, UserContext}
import org.silkframework.util.{Identifier, MockitoSugar}
import org.silkframework.workspace.activity.workflow.WorkflowTest._
import org.silkframework.workspace.resources.InMemoryResourceRepository
import org.silkframework.workspace._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.rule.{DatasetSelection, TransformSpec}
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext, TestWriteContext, WriteContext, XmlSerialization}
import org.silkframework.workspace.activity.workflow.WorkflowNode.convertStringToOption

import scala.xml.{Attribute, Elem, Node, Null, Text}

class WorkflowTest extends AnyFlatSpec with MockitoSugar with Matchers with TestUserContextTrait {
  behavior of "Workflow"

  it should "support sorting its workflow operators topologically" in {
    val project = mock[Project]
    val workflow = testWorkflow
    for (dataset <- workflow.datasets) {
      val id = Identifier(dataset.nodeId)
      val datasetTask = mock[ProjectTask[GenericDatasetSpec]]
      when(datasetTask.id).thenReturn(id)
      implicit val userContext: UserContext = UserContext.Empty
      when(project.taskOption[GenericDatasetSpec](dataset.task)).thenReturn(Some(datasetTask))
    }
    val sortedWorkflowNodes = workflow.topologicalSortedNodes.map(_.nodeId)
    sortedWorkflowNodes mustBe Seq(DS_A1, DS_A2, TRANSFORM_1, TRANSFORM_2, DS_B1, DS_B2, LINKING, LINKS, GENERATE_OUTPUT, OUTPUT)
  }

  it should "detect circular workflows" in {
    intercept[RuntimeException] {
      circularWorkflow.topologicalSortedNodes
    }
    intercept[RuntimeException] {
      circularWorkflow.workflowDependencyGraph
    }
    intercept[RuntimeException] {
      circularWorkflowWithDependencies.topologicalSortedNodes
    }
    intercept[RuntimeException] {
      circularWorkflowWithDependencies.workflowDependencyGraph
    }
  }

  it should "generate a DAG of the node dependencies" in {
    val dag = testWorkflow.workflowDependencyGraph
    dag mustBe WorkflowDependencyGraph(
      startNodes = Seq(
        WorkflowDependencyNode(WorkflowDataset(List(), DS_A1, List(TRANSFORM_1), (0, 0), DS_A1, Seq.empty, Seq.empty)),
        WorkflowDependencyNode(WorkflowDataset(List(), DS_A2, List(TRANSFORM_2), (0, 0), DS_A2, Seq.empty, Seq.empty))),
      endNodes = Seq(
        WorkflowDependencyNode(WorkflowDataset(List(Some(TRANSFORM_2)), DS_B, List(), (0, 0), DS_B2, Seq.empty, Seq.empty)),
        WorkflowDependencyNode(WorkflowDataset(List(Some(GENERATE_OUTPUT)), OUTPUT, List(), (0, 0), OUTPUT, Seq.empty, Seq.empty))
      ))
    val dsA1 = dag.startNodes.filter(_.workflowNode.nodeId == DS_A1).head
    intercept[IllegalStateException] {
      dsA1.addFollowingNode(null)
    }
    var current = dsA1
    for (nextLabel <- Seq(TRANSFORM_1, DS_B1, LINKING, LINKS, GENERATE_OUTPUT, OUTPUT)) {
      current.followingNodes.size mustBe 1
      val next = current.followingNodes.head
      next.nodeId mustBe nextLabel
      next.precedingNodes must contain(current)
      if (!next.workflowNode.isInstanceOf[WorkflowDataset]) {
        next.inputNodes must contain(current)
      }
      current = next
    }
    dag.endNodes must contain (current)
  }

  it should "sort correctly for a workflow ending in an operator" in {
    val sortedNodes = testWorkflowEndingInOperator.topologicalSortedNodes
    sortedNodes.map(_.nodeId) mustBe Seq(DS_A, DS_B, TRANSFORM)
  }

  it should "create a correct DAG for a workflow with dependency connections" in {
    val dag = workflowWithDependenciesBetweenOtherwiseIndependentWorkflowBranches.workflowDependencyGraph
    dag.startNodes.map(_.nodeId) mustBe Iterable(DS_A)
    dag.endNodes.map(_.nodeId) mustBe Seq(DS_B2)
  }

  it should "build the DAG correctly for a workflow ending in an operator" in {
    val dag = testWorkflowEndingInOperator.workflowDependencyGraph
    dag.startNodes.map(_.nodeId) mustBe Seq(DS_A, DS_B)
    dag.endNodes.map(_.nodeId) mustBe Seq(TRANSFORM)
  }

  it should "sort correctly for a workflow with disjunct data flows and multiple output nodes" in {
    val sortedNodes = testWorkflowWithMultipleEndNodesAndDisjunctDataFlows.topologicalSortedNodes
    sortedNodes.map(_.nodeId) mustBe Seq(DS_A, DS_B, DS_C, TRANSFORM, OP_1, OP_2)
  }

  it should "build the DAG correctly for a workflow with disjunct data flows and multiple output nodes" in {
    val dag = testWorkflowWithMultipleEndNodesAndDisjunctDataFlows.workflowDependencyGraph
    dag.startNodes.map(_.nodeId) mustBe Seq(DS_A, DS_B, DS_C)
    dag.endNodes.map(_.nodeId) mustBe Seq(OP_1, OP_2, TRANSFORM)
  }

  it should "put workflow nodes that have neither input nor output into the end nodes" in {
    singleNodeWorkflow.workflowDependencyGraph.endNodes.map(_.nodeId) mustBe Seq(OP_1)
  }

  it should "sort and build the DAG correctly for a workflow with config inputs" in {
    val sortedWorkflowNodes = testWorkflowWithConfigInputs.topologicalSortedNodes.map(_.nodeId)
    sortedWorkflowNodes mustBe Seq(DS_A1, DS_A2, TRANSFORM_2, CONFIG_NODE, TRANSFORM_1, DS_B2, DS_B1, LINKING, LINKS, GENERATE_OUTPUT, OUTPUT)
    val dsA1 = testWorkflowWithConfigInputs.workflowDependencyGraph.startNodes.find(_.nodeId == DS_A1)
    dsA1 mustBe defined
    dsA1.get.followingNodes.exists(_.nodeId == CONFIG_NODE) mustBe true
    // Check if config node is preceding node of transformation
    dsA1.get.followingNodes.find(_.nodeId == TRANSFORM_1).get.precedingNodes.exists(_.nodeId == CONFIG_NODE) mustBe true
  }

  it should "resolve replaceable datasets correctly" in {
    val workspace = new Workspace(new InMemoryWorkspaceProvider(), InMemoryResourceRepository())
    val project = workspace.createProject(ProjectConfig("projectA"))
    val resource = project.resources.get("doesntMatter")
    for(op <- testWorkflow.operators) {
      project.addTask[GenericDatasetSpec](op.task, DatasetSpec(CsvDataset(resource)))
    }
    testWorkflow.copy(
      replaceableInputs = Seq(),
      replaceableOutputs = Seq()
    ).markedReplaceableDatasets(project) mustBe AllReplaceableDatasets(Seq(), Seq())
    intercept[IllegalArgumentException] {
      testWorkflow.copy(
        replaceableInputs = Seq(),
        replaceableOutputs = Seq(DS_B)
      ).markedReplaceableDatasets(project)
    }
    intercept[IllegalArgumentException] {
      testWorkflow.copy(
        replaceableInputs = Seq(DS_B),
        replaceableOutputs = Seq()
      ).markedReplaceableDatasets(project)
    }
    testWorkflow.copy(
      replaceableInputs = Seq(DS_A1),
      replaceableOutputs = Seq(OUTPUT)
    ).markedReplaceableDatasets(project) mustBe AllReplaceableDatasets(Seq(DS_A1), Seq(OUTPUT))
    workflowWithConfigOnlyInput.copy(
      replaceableInputs = Seq(DS_A),
      replaceableOutputs = Seq()
    ).markedReplaceableDatasets(project) mustBe AllReplaceableDatasets(Seq(DS_A), Seq())
  }

  it should "not return a re-configured input dataset as output dataset" in {
    val workspace = new Workspace(new InMemoryWorkspaceProvider(), InMemoryResourceRepository())
    val project = workspace.createProject(ProjectConfig("projectA"))
    for(datasetId <- Seq(DS_A, DS_B)) {
      val dataset = CsvDataset(project.resources.get("file.csv"))
      project.addTask[GenericDatasetSpec](datasetId, DatasetSpec(dataset))
    }
    for(transformId <- Seq(TRANSFORM_1, TRANSFORM_2)) {
      project.addTask[TransformSpec](transformId, TransformSpec(DatasetSelection(DS_A)))
    }
    reConfiguredDatasetWorkflow.outputDatasets(project).map(_.id.toString) mustBe Seq(DS_B)
  }

  it should "reject replaceable dataset IDs of datasets that are not part of the workflow" in {
    intercept[IllegalArgumentException] {
      testWorkflow.copy(replaceableInputs = Seq(DS_A1, "removedDataset"))
    }
    intercept[IllegalArgumentException] {
      testWorkflow.copy(replaceableOutputs = Seq("removedDataset"))
    }
  }

  it should "drop replaceable dataset IDs of datasets that are not part of the workflow on creation" in {
    // Stale IDs remain if a replaceable dataset is removed from the workflow or deleted, e.g. in the workflow editor
    val workflow = Workflow.createNormalized(
      operators = testWorkflow.operators,
      datasets = testWorkflow.datasets,
      replaceableInputs = Seq(DS_A1, "removedDataset"),
      replaceableOutputs = Seq(OUTPUT, "removedDataset")
    )
    workflow.replaceableInputs.taskIds mustBe Seq(DS_A1)
    workflow.replaceableOutputs.taskIds mustBe Seq(OUTPUT)
  }

  it should "drop stale replaceable dataset IDs when reading XML" in {
    implicit val readContext: ReadContext = TestReadContext()
    implicit val writeContext: WriteContext[Node] = TestWriteContext[Node]()
    // XML from old exports may still contain IDs of datasets that were removed from the workflow
    val staleXml = XmlSerialization.toXml(testWorkflow).asInstanceOf[Elem] %
      Attribute("replaceableInputs", Text(s"$DS_A1,removedDataset"), Null) %
      Attribute("replaceableOutputs", Text("removedDataset"), Null)
    val workflow = XmlSerialization.fromXml[Workflow](staleXml)
    workflow.replaceableInputs.taskIds mustBe Seq(DS_A1)
    workflow.replaceableOutputs.taskIds mustBe Seq.empty
    (XmlSerialization.toXml(workflow) \ "@replaceableInputs").text mustBe DS_A1
  }

  it should "not return datasets as output datasets that only have tasks as inputs that generate no data" in {
    val workspace = new Workspace(new InMemoryWorkspaceProvider(), InMemoryResourceRepository())
    val project = workspace.createProject(ProjectConfig("projectA"))
    for (datasetId <- Seq(DS_A, DS_B, DS_B2)) {
      val dataset = CsvDataset(project.resources.get("file.csv"))
      project.addTask[GenericDatasetSpec](datasetId, DatasetSpec(dataset))
    }
    project.addTask[Workflow](WORKFLOW, Workflow())
    project.addTask[TransformSpec](TRANSFORM_2, TransformSpec(DatasetSelection(DS_A)))
    noSchemaInputDatasetWorkflow.outputDatasets(project).map(_.id.toString) mustBe Seq(DS_B, DS_A)
  }
}

object WorkflowTest {
  val DS_A = "dsA"
  val DS_A1 = "dsA1"
  val DS_A2 = "dsA2"
  val DS_B = "dsB"
  val DS_B1 = "dsB1"
  val DS_B2 = "dsB2"
  val TRANSFORM = "transform"
  val TRANSFORM_1 = "transform1"
  val TRANSFORM_2 = "transform2"
  val LINKS = "links"
  val OUTPUT = "output"
  val LINKING = "linking"
  val GENERATE_OUTPUT = "generateOutput"
  val WORKFLOW = "workflow"
  val OP_1 = "op1"
  val OP_2 = "op2"
  val DS_C = "dsC"
  val CONFIG_NODE = "conf"

  val testWorkflow: Workflow = {
    Workflow(
      operators = Seq(
        operator(task = TRANSFORM_1, inputs = Seq(DS_A1), outputs = Seq(DS_B1), TRANSFORM_1),
        operator(task = TRANSFORM_2, inputs = Seq(DS_A2), outputs = Seq(DS_B1), TRANSFORM_2),
        operator(task = LINKING, inputs = Seq(DS_B1, DS_B1), outputs = Seq(LINKS), LINKING),
        operator(task = GENERATE_OUTPUT, inputs = Seq(LINKS), outputs = Seq(OUTPUT), GENERATE_OUTPUT)
      ),
      datasets = Seq(
        dataset(DS_A1, DS_A1, outputs = Seq(TRANSFORM_1)),
        dataset(DS_A2, DS_A2, outputs = Seq(TRANSFORM_2)),
        dataset(DS_B, DS_B1, inputs = Seq(TRANSFORM_1), outputs = Seq(LINKING, LINKING)),
        dataset(DS_B, DS_B2, inputs = Seq(TRANSFORM_2)),
        dataset(LINKS, LINKS, inputs = Seq(LINKING), outputs = Seq(GENERATE_OUTPUT)),
        dataset(OUTPUT, OUTPUT, inputs = Seq(GENERATE_OUTPUT))
      ))
  }

  val testWorkflowWithConfigInputs: Workflow = {
    Workflow(
      operators = testWorkflow.operators.map(op => if (op.nodeId != TRANSFORM_1) op else op.copy(configInputs = Seq(CONFIG_NODE))) ++ Seq(
        operator(task = CONFIG_NODE, inputs = Seq(DS_A1), outputs = Seq(TRANSFORM_1), CONFIG_NODE)
      ),
      datasets = testWorkflow.datasets
    )
  }

  val testWorkflowEndingInOperator: Workflow = {
    Workflow(
      operators = Seq(
        operator(task = TRANSFORM, inputs = Seq(DS_A, DS_B), outputs = Seq(), TRANSFORM)
      ),
      datasets = Seq(
        dataset(DS_A, DS_A, outputs = Seq(TRANSFORM)),
        dataset(DS_B, DS_B, outputs = Seq(TRANSFORM))
      ))
  }

  val testWorkflowWithMultipleEndNodesAndDisjunctDataFlows: Workflow = {
    Workflow(
      operators = Seq(
        operator(task = TRANSFORM, inputs = Seq(DS_A, DS_B), outputs = Seq(), TRANSFORM),
        operator(task = OP_1, inputs = Seq(DS_C), outputs = Seq(), OP_1),
        operator(task = OP_2, inputs = Seq(DS_C), outputs = Seq(), OP_2)
      ),
      datasets = Seq(
        dataset(DS_A, DS_A, outputs = Seq(TRANSFORM)),
        dataset(DS_B, DS_B, outputs = Seq(TRANSFORM)),
        dataset(DS_C, DS_C, outputs = Seq(OP_1, OP_2))
      ))
  }

  val singleNodeWorkflow: Workflow = {
    Workflow(
      operators = Seq(
        operator(task = OP_1, inputs = Seq(), outputs = Seq(), OP_1)
      ),
      datasets = Seq(
      ))
  }

  val circularWorkflow: Workflow = {
    Workflow(
      operators = Seq(
        operator(task = TRANSFORM_1, inputs = Seq(TRANSFORM_2), outputs = Seq(TRANSFORM_2), TRANSFORM_1),
        operator(task = TRANSFORM_2, inputs = Seq(TRANSFORM_1), outputs = Seq(TRANSFORM_1), TRANSFORM_2)
      ),
      datasets = Seq()
    )
  }

  // Circular workflow using dependencyInputs
  val circularWorkflowWithDependencies: Workflow = {
    Workflow(
      operators = Seq(
        operator(task = TRANSFORM_1, inputs = Seq(DS_A), outputs = Seq(DS_B), TRANSFORM_1, dependencyInputs = Seq(DS_B2)),
        operator(task = TRANSFORM_2, inputs = Seq(DS_A1), outputs = Seq(DS_B2), TRANSFORM_2)
      ),
      datasets = Seq(
        dataset(DS_A, DS_A, outputs = Seq(TRANSFORM_1)),
        dataset(DS_B, DS_B, inputs = Seq(TRANSFORM_1)),
        dataset(DS_A1, DS_A1, outputs = Seq(TRANSFORM_2), dependencyInputs = Seq(DS_B)),
        dataset(DS_B2, DS_B2, inputs = Seq(TRANSFORM_2))
      )
    )
  }

  val workflowWithDependenciesBetweenOtherwiseIndependentWorkflowBranches: Workflow = circularWorkflowWithDependencies.copy(
    operators = Seq(
      operator(task = TRANSFORM_1, inputs = Seq(DS_A), outputs = Seq(DS_B), TRANSFORM_1),
      operator(task = TRANSFORM_2, inputs = Seq(DS_A1), outputs = Seq(DS_B2), TRANSFORM_2)
    )
  )

  val reConfiguredDatasetWorkflow: Workflow = {
    Workflow(
      operators = Seq(
        operator(task = TRANSFORM_1, inputs = Seq(), outputs = Seq(DS_A, DS_B), TRANSFORM_1),
        operator(task = TRANSFORM_2, inputs = Seq(), outputs = Seq(DS_B), TRANSFORM_2),
      ),
      datasets = Seq(
        dataset(DS_A, DS_A, outputs = Seq(), configInputs = Seq(TRANSFORM_1)),
        dataset(DS_B, DS_B, inputs = Seq(TRANSFORM_2), outputs = Seq(), configInputs = Seq(TRANSFORM_1))
      )
    )
  }

  val noSchemaInputDatasetWorkflow: Workflow = {
    Workflow(
      operators = Seq(
        operator(task = TRANSFORM_2, inputs = Seq(DS_A), outputs = Seq(DS_B), TRANSFORM_2),
        operator(task = WORKFLOW, inputs = Seq(), outputs = Seq(DS_A), WORKFLOW)
      ),
      datasets = Seq(
        dataset(DS_A, DS_A, inputs = Seq(WORKFLOW, DS_B2), outputs = Seq(TRANSFORM_2)),
        dataset(DS_B, DS_B, inputs = Seq(TRANSFORM_2), outputs = Seq()),
        dataset(DS_B2, DS_B2, inputs = Seq(), outputs = Seq(DS_A))
      )
    )
  }

  // Workflow with a dataset that is re-configured by another dataset that should be recognized as replaceable input.
  val workflowWithConfigOnlyInput: Workflow = {
    Workflow(
      operators = Seq(
        operator(task = TRANSFORM_1, inputs = Seq(DS_B), outputs = Seq(), TRANSFORM_1),
      ),
      datasets = Seq(
        dataset(DS_A, DS_A, inputs = Seq(), outputs = Seq(DS_B)),
        dataset(DS_B, DS_B, inputs = Seq(), configInputs = Seq(DS_A), outputs = Seq()),
      )
    )
  }

  def operator(task: String, inputs: Seq[String], outputs: Seq[String], nodeId: String,
               dependencyInputs: Seq[String] = Seq.empty): WorkflowOperator = {
    WorkflowOperator(inputs = inputs.map(convertStringToOption), task = task, outputs = outputs, Seq(), (0, 0), nodeId, Seq.empty, dependencyInputs)
  }

  def dataset(task: String,
              nodeId: String,
              inputs: Seq[String] = Seq.empty,
              outputs: Seq[String] = Seq.empty,
              configInputs: Seq[String] = Seq.empty,
              dependencyInputs: Seq[String] = Seq.empty): WorkflowDataset = {
    WorkflowDataset(inputs.map(convertStringToOption), task, outputs, (0, 0), nodeId, configInputs, dependencyInputs)
  }
}