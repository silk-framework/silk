package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{ConfigTest, MetaData, Prefixes}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.plugins.dataset.rdf.tasks.SparqlCopyCustomTask
import org.silkframework.rule._
import org.silkframework.runtime.activity.{ActivityMonitor, UserContext}
import org.silkframework.util.{ConfigTestTrait, Uri}
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow, WorkflowDataset, WorkflowExecutionReportWithProvenance, WorkflowOperator}
import org.silkframework.workspace.{InMemoryWorkspaceTestTrait, ProjectConfig, WorkspaceFactory}

/**
 * Integration test for [[InWorkflowDataset]] within a real workflow execution.
 *
 * Tests that:
 * - Two InWorkflowDataset instances are fully isolated from each other.
 * - Multiple uses of the same InWorkflowDataset instance within one workflow
 *   execution all see the same data.
 *
 * Workflow structure:
 *   source1 → copyToInWorkflow1 → inWorkflow1 → readFromInWorkflow1A → output1A
 *                                      └──────→ readFromInWorkflow1B → output1B
 *   source2 → copyToInWorkflow2 → inWorkflow2 → readFromInWorkflow2A → output2A
 *                                      └──────→ readFromInWorkflow2B → output2B
 *
 * Writing to each InWorkflowDataset goes through SparqlCopyCustomTask (QuadEntitySchema →
 * withEntitySink → access() → executor model). Reading goes through TransformSpec
 * (FixedSchemaPort(MultiEntitySchema) → handleMultiEntitySchema → access().source →
 * executor model), which is the only read path that correctly reaches the executor model.
 */
class InWorkflowDatasetIntegrationTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  implicit val userContext: UserContext = UserContext.Empty
  implicit val prefixes: Prefixes = Prefixes.empty

  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.provider.plugin" -> Some("inMemoryWorkspaceProvider")
  )

  "InWorkflowDataset" should "isolate data between two instances and share data across multiple uses of the same instance within a workflow" in {
    val workspace = WorkspaceFactory().workspace
    val project = workspace.createProject(ProjectConfig(metaData = MetaData(Some("inWorkflowIntegrationTest"))))

    // Source datasets pre-populated with distinct triples.
    val source1Model: Model = ModelFactory.createDefaultModel()
    source1Model.createResource("http://s1")
      .addProperty(source1Model.createProperty("http://p"), source1Model.createResource("http://o1"))

    val source2Model: Model = ModelFactory.createDefaultModel()
    source2Model.createResource("http://s2")
      .addProperty(source2Model.createProperty("http://p"), source2Model.createResource("http://o2"))

    // Output datasets — empty initially, filled by the workflow.
    val output1AModel: Model = ModelFactory.createDefaultModel()
    val output1BModel: Model = ModelFactory.createDefaultModel()
    val output2AModel: Model = ModelFactory.createDefaultModel()
    val output2BModel: Model = ModelFactory.createDefaultModel()

    // Register dataset tasks.
    project.addTask("source1",     DatasetSpec(JenaModelDataset(source1Model)))
    project.addTask("source2",     DatasetSpec(JenaModelDataset(source2Model)))
    project.addTask("inWorkflow1", DatasetSpec(InWorkflowDataset()))
    project.addTask("inWorkflow2", DatasetSpec(InWorkflowDataset()))
    project.addTask("output1A",    DatasetSpec(JenaModelDataset(output1AModel)))
    project.addTask("output1B",    DatasetSpec(JenaModelDataset(output1BModel)))
    project.addTask("output2A",    DatasetSpec(JenaModelDataset(output2AModel)))
    project.addTask("output2B",    DatasetSpec(JenaModelDataset(output2BModel)))

    // SparqlCopyCustomTask: reads via SparqlEndpointEntitySchema, outputs QuadEntitySchema.
    // Quads are written to InWorkflowDataset via withEntitySink → access() → executor model.
    val copyQuery = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }"
    project.addTask("copyToInWorkflow1", SparqlCopyCustomTask(copyQuery, tempFile = false))
    project.addTask("copyToInWorkflow2", SparqlCopyCustomTask(copyQuery, tempFile = false))

    // TransformSpec: reads via FixedSchemaPort(MultiEntitySchema) → handleMultiEntitySchema
    // → access().source → executor model. Identity mapping preserves the <http://p> property.
    val identityTransform = TransformSpec(
      selection = DatasetSelection("dummy", Uri("")),
      mappingRule = RootMappingRule(MappingRules(
        propertyRules = Seq(
          DirectMapping(
            id = "pmap",
            sourcePath = UntypedPath(Uri("http://p")),
            mappingTarget = MappingTarget(Uri("http://p"))
          )
        )
      ))
    )
    project.addTask("readFromInWorkflow1A", identityTransform)
    project.addTask("readFromInWorkflow1B", identityTransform)
    project.addTask("readFromInWorkflow2A", identityTransform)
    project.addTask("readFromInWorkflow2B", identityTransform)

    // Build the workflow.
    // inWorkflow1 is written to once (by copyToInWorkflow1) and then read twice
    // (by readFromInWorkflow1A and readFromInWorkflow1B), exercising the
    // alreadyExecuted / multiple-reads behaviour.
    val workflow = Workflow(
      operators = Seq(
        WorkflowOperator(Seq(Some("source1")),     "copyToInWorkflow1",   Seq("inWorkflow1"), Seq.empty, (0,   0), "copyToInWorkflow1",   None, Seq.empty, Seq.empty),
        WorkflowOperator(Seq(Some("source2")),     "copyToInWorkflow2",   Seq("inWorkflow2"), Seq.empty, (0, 300), "copyToInWorkflow2",   None, Seq.empty, Seq.empty),
        WorkflowOperator(Seq(Some("inWorkflow1")), "readFromInWorkflow1A", Seq("output1A"),   Seq.empty, (200,  0), "readFromInWorkflow1A", None, Seq.empty, Seq.empty),
        WorkflowOperator(Seq(Some("inWorkflow1")), "readFromInWorkflow1B", Seq("output1B"),   Seq.empty, (200,100), "readFromInWorkflow1B", None, Seq.empty, Seq.empty),
        WorkflowOperator(Seq(Some("inWorkflow2")), "readFromInWorkflow2A", Seq("output2A"),   Seq.empty, (200,300), "readFromInWorkflow2A", None, Seq.empty, Seq.empty),
        WorkflowOperator(Seq(Some("inWorkflow2")), "readFromInWorkflow2B", Seq("output2B"),   Seq.empty, (200,400), "readFromInWorkflow2B", None, Seq.empty, Seq.empty)
      ),
      datasets = Seq(
        WorkflowDataset(Seq.empty,                        "source1",     Seq("copyToInWorkflow1"),                          (0,   0), "source1",     None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq.empty,                        "source2",     Seq("copyToInWorkflow2"),                          (0, 300), "source2",     None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("copyToInWorkflow1")),   "inWorkflow1", Seq("readFromInWorkflow1A", "readFromInWorkflow1B"), (100,  0), "inWorkflow1", None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("copyToInWorkflow2")),   "inWorkflow2", Seq("readFromInWorkflow2A", "readFromInWorkflow2B"), (100,300), "inWorkflow2", None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("readFromInWorkflow1A")), "output1A",  Seq.empty,                                          (300,  0), "output1A",    None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("readFromInWorkflow1B")), "output1B",  Seq.empty,                                          (300,100), "output1B",    None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("readFromInWorkflow2A")), "output2A",  Seq.empty,                                          (300,300), "output2A",    None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("readFromInWorkflow2B")), "output2B",  Seq.empty,                                          (300,400), "output2B",    None, Seq.empty, Seq.empty)
      )
    )
    project.addTask("workflow", workflow)
    val workflowTask = project.task[Workflow]("workflow")

    // Execute the workflow.
    val executor = LocalWorkflowExecutorGeneratingProvenance(workflowTask)
    val monitor = new ActivityMonitor("monitor", initialValue = Some(WorkflowExecutionReportWithProvenance.empty))
    executor.run(monitor)

    // Each InWorkflowDataset instance received data and fed its downstream operators.
    output1AModel.size() must be > 0L
    output1BModel.size() must be > 0L
    output2AModel.size() must be > 0L
    output2BModel.size() must be > 0L

    // Multiple uses of the same instance: both reads of inWorkflow1 see identical data.
    output1AModel.isIsomorphicWith(output1BModel) mustBe true

    // Multiple uses of the same instance: both reads of inWorkflow2 see identical data.
    output2AModel.isIsomorphicWith(output2BModel) mustBe true

    // Isolation: inWorkflow1 (source1 data) and inWorkflow2 (source2 data) are separate.
    output1AModel.isIsomorphicWith(output2AModel) mustBe false
  }
}
