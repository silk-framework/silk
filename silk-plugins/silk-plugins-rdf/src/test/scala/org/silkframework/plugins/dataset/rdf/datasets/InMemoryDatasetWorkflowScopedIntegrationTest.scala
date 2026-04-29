package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{MetaData, Prefixes}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.plugins.dataset.rdf.tasks.SparqlCopyCustomTask
import org.silkframework.rule._
import org.silkframework.runtime.activity.{ActivityMonitor, UserContext}
import org.silkframework.util.{ConfigTestTrait, Uri}
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow, WorkflowDataset, WorkflowExecutionReportWithProvenance, WorkflowOperator}
import org.silkframework.workspace.{InMemoryWorkspaceTestTrait, ProjectConfig, WorkspaceFactory}

/**
 * Integration test for [[InMemoryDataset]] with `workflowScoped = true` within a real workflow execution.
 */
class InMemoryDatasetWorkflowScopedIntegrationTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  implicit val userContext: UserContext = UserContext.Empty
  implicit val prefixes: Prefixes = Prefixes.empty

  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.provider.plugin" -> Some("inMemoryWorkspaceProvider")
  )

  "InMemoryDataset (workflowScoped = true)" should "isolate data between two instances and share data across multiple uses of the same instance within a workflow" in {
    val workspace = WorkspaceFactory().workspace
    val project = workspace.createProject(ProjectConfig(metaData = MetaData(Some("inMemoryWorkflowScopedIntegrationTest"))))

    val source1Model: Model = ModelFactory.createDefaultModel()
    source1Model.createResource("http://s1")
      .addProperty(source1Model.createProperty("http://p"), source1Model.createResource("http://o1"))

    val source2Model: Model = ModelFactory.createDefaultModel()
    source2Model.createResource("http://s2")
      .addProperty(source2Model.createProperty("http://p"), source2Model.createResource("http://o2"))

    val output1AModel: Model = ModelFactory.createDefaultModel()
    val output1BModel: Model = ModelFactory.createDefaultModel()
    val output2AModel: Model = ModelFactory.createDefaultModel()
    val output2BModel: Model = ModelFactory.createDefaultModel()

    project.addTask("source1",      DatasetSpec(JenaModelDataset.fromModel(source1Model)))
    project.addTask("source2",      DatasetSpec(JenaModelDataset.fromModel(source2Model)))
    project.addTask("inMemory1",    DatasetSpec(InMemoryDataset(workflowScoped = true)))
    project.addTask("inMemory2",    DatasetSpec(InMemoryDataset(workflowScoped = true)))
    project.addTask("output1A",     DatasetSpec(JenaModelDataset.fromModel(output1AModel)))
    project.addTask("output1B",     DatasetSpec(JenaModelDataset.fromModel(output1BModel)))
    project.addTask("output2A",     DatasetSpec(JenaModelDataset.fromModel(output2AModel)))
    project.addTask("output2B",     DatasetSpec(JenaModelDataset.fromModel(output2BModel)))

    val copyQuery = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }"
    project.addTask("copyToInMemory1", SparqlCopyCustomTask(copyQuery, tempFile = false))
    project.addTask("copyToInMemory2", SparqlCopyCustomTask(copyQuery, tempFile = false))

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
    project.addTask("readFromInMemory1A", identityTransform)
    project.addTask("readFromInMemory1B", identityTransform)
    project.addTask("readFromInMemory2A", identityTransform)
    project.addTask("readFromInMemory2B", identityTransform)

    val workflow = Workflow(
      operators = Seq(
        WorkflowOperator(Seq(Some("source1")),    "copyToInMemory1",    Seq("inMemory1"), Seq.empty, (0,   0), "copyToInMemory1",    None, Seq.empty, Seq.empty),
        WorkflowOperator(Seq(Some("source2")),    "copyToInMemory2",    Seq("inMemory2"), Seq.empty, (0, 300), "copyToInMemory2",    None, Seq.empty, Seq.empty),
        WorkflowOperator(Seq(Some("inMemory1")),  "readFromInMemory1A", Seq("output1A"),  Seq.empty, (200,  0), "readFromInMemory1A", None, Seq.empty, Seq.empty),
        WorkflowOperator(Seq(Some("inMemory1")),  "readFromInMemory1B", Seq("output1B"),  Seq.empty, (200,100), "readFromInMemory1B", None, Seq.empty, Seq.empty),
        WorkflowOperator(Seq(Some("inMemory2")),  "readFromInMemory2A", Seq("output2A"),  Seq.empty, (200,300), "readFromInMemory2A", None, Seq.empty, Seq.empty),
        WorkflowOperator(Seq(Some("inMemory2")),  "readFromInMemory2B", Seq("output2B"),  Seq.empty, (200,400), "readFromInMemory2B", None, Seq.empty, Seq.empty)
      ),
      datasets = Seq(
        WorkflowDataset(Seq.empty,                         "source1",   Seq("copyToInMemory1"),                            (0,   0), "source1",   None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq.empty,                         "source2",   Seq("copyToInMemory2"),                            (0, 300), "source2",   None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("copyToInMemory1")),      "inMemory1", Seq("readFromInMemory1A", "readFromInMemory1B"),   (100,  0), "inMemory1", None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("copyToInMemory2")),      "inMemory2", Seq("readFromInMemory2A", "readFromInMemory2B"),   (100,300), "inMemory2", None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("readFromInMemory1A")),   "output1A",  Seq.empty,                                         (300,  0), "output1A",  None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("readFromInMemory1B")),   "output1B",  Seq.empty,                                         (300,100), "output1B",  None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("readFromInMemory2A")),   "output2A",  Seq.empty,                                         (300,300), "output2A",  None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("readFromInMemory2B")),   "output2B",  Seq.empty,                                         (300,400), "output2B",  None, Seq.empty, Seq.empty)
      )
    )
    project.addTask("workflow", workflow)
    val workflowTask = project.task[Workflow]("workflow")

    val executor = LocalWorkflowExecutorGeneratingProvenance(workflowTask)
    val monitor = new ActivityMonitor("monitor", initialValue = Some(WorkflowExecutionReportWithProvenance.empty))
    executor.run(monitor)

    output1AModel.size() must be > 0L
    output1BModel.size() must be > 0L
    output2AModel.size() must be > 0L
    output2BModel.size() must be > 0L

    output1AModel.isIsomorphicWith(output1BModel) mustBe true
    output2AModel.isIsomorphicWith(output2BModel) mustBe true
    output1AModel.isIsomorphicWith(output2AModel) mustBe false
  }

  it should "propagate InMemoryDataset (workflowScoped) data from a parent workflow to a nested workflow" in {
    val workspace = WorkspaceFactory().workspace
    val project = workspace.createProject(ProjectConfig(metaData = MetaData(Some("nestedWorkflowScopedTest"))))

    val sourceModel: Model = ModelFactory.createDefaultModel()
    sourceModel.createResource("http://nested/s1")
      .addProperty(sourceModel.createProperty("http://p"), sourceModel.createResource("http://nested/o1"))
    sourceModel.createResource("http://nested/s2")
      .addProperty(sourceModel.createProperty("http://p"), sourceModel.createResource("http://nested/o2"))

    val outputModel: Model = ModelFactory.createDefaultModel()

    project.addTask("source",    DatasetSpec(JenaModelDataset.fromModel(sourceModel)))
    project.addTask("inMemoryDs", DatasetSpec(InMemoryDataset(workflowScoped = true)))
    project.addTask("output",    DatasetSpec(JenaModelDataset.fromModel(outputModel)))

    val copyQuery = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }"
    project.addTask("copyToInMemory", SparqlCopyCustomTask(copyQuery, tempFile = false))

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
    project.addTask("readFromInMemory", identityTransform)

    val nestedWorkflow = Workflow(
      operators = Seq(
        WorkflowOperator(Seq(Some("inMemoryDs")), "readFromInMemory", Seq("output"), Seq.empty, (100, 0), "readFromInMemory", None, Seq.empty, Seq.empty)
      ),
      datasets = Seq(
        WorkflowDataset(Seq.empty,                      "inMemoryDs", Seq("readFromInMemory"), (0, 0),   "inMemoryDs", None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("readFromInMemory")),  "output",     Seq.empty,               (200, 0), "output",     None, Seq.empty, Seq.empty)
      )
    )
    project.addTask("nestedWorkflow", nestedWorkflow)

    val parentWorkflow = Workflow(
      operators = Seq(
        WorkflowOperator(Seq(Some("source")),       "copyToInMemory", Seq("inMemoryDs"), Seq.empty, (100, 0), "copyToInMemory", None, Seq.empty, Seq.empty),
        WorkflowOperator(Seq(Some("inMemoryDs")),   "nestedWorkflow", Seq.empty,         Seq.empty, (300, 0), "nestedWorkflow", None, Seq.empty, Seq.empty)
      ),
      datasets = Seq(
        WorkflowDataset(Seq.empty,                    "source",     Seq("copyToInMemory"),  (0,   0), "source",     None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some("copyToInMemory")),  "inMemoryDs", Seq("nestedWorkflow"),  (200, 0), "inMemoryDs", None, Seq.empty, Seq.empty)
      )
    )
    project.addTask("parentWorkflow", parentWorkflow)
    val workflowTask = project.task[Workflow]("parentWorkflow")

    val executor = LocalWorkflowExecutorGeneratingProvenance(workflowTask)
    val monitor = new ActivityMonitor("nestedMonitor", initialValue = Some(WorkflowExecutionReportWithProvenance.empty))
    executor.run(monitor)

    outputModel.size() must be > 0L
    outputModel.listSubjects().toList.size() mustBe 2
  }
}
