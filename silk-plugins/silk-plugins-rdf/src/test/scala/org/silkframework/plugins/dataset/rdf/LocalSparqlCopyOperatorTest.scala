package org.silkframework.plugins.dataset.rdf

  import org.silkframework.config.{PlainTask, Prefixes}
  import org.silkframework.dataset.DatasetSpec
  import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
  import org.silkframework.dataset.rdf.SparqlEndpointEntityTable
  import org.silkframework.execution.ExecutorOutput
  import org.silkframework.execution.local.LocalExecution
  import org.silkframework.plugins.dataset.rdf.datasets.RdfFileDataset
  import org.silkframework.plugins.dataset.rdf.executors.LocalSparqlCopyExecutor
  import org.silkframework.plugins.dataset.rdf.tasks.SparqlCopyCustomTask
  import org.silkframework.runtime.activity.{ActivityMonitor, UserContext}
  import org.silkframework.runtime.resource.{ClasspathResourceLoader, ReadOnlyResourceManager}
  import org.silkframework.util.{MockitoSugar, TestMocks}
  import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow, WorkflowExecutionReportWithProvenance}
  import org.silkframework.workspace.{SingleProjectWorkspaceProviderTestTrait, WorkspaceFactory}

  import java.io.File
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

  class LocalSparqlCopyOperatorTest extends AnyFlatSpec with Matchers with MockitoSugar with SingleProjectWorkspaceProviderTestTrait  {
    behavior of "Local SPARQL Copy Executor"

    implicit val uc: UserContext = UserContext.Empty
    implicit val prefixes: Prefixes = Prefixes.empty

    private lazy val resources = ReadOnlyResourceManager(ClasspathResourceLoader(getClass.getPackage.getName.replace('.', '/')))

    /**
      * Returns the path of the XML zip project that should be loaded before the test suite starts.
      */
    override def projectPathInClasspath: String = "org/silkframework/plugins/dataset/rdf/sparqlCopyProject.zip"

    /** The workspace provider that is used for holding the test workspace. */
    override def workspaceProviderId: String = "inMemory"

    // execution data
    private val execution = LocalExecution(true)
    private val executor = new LocalSparqlCopyExecutor()
    private val constructQuery = "CONSTRUCT { ?s ?p ?o. } WHERE { ?s ?p ?o. FILTER(?s = <http://dbpedia.org/resource/Albert_Einstein>) }"
    private val context = TestMocks.activityContextMock()
    private val source = RdfFileDataset(resources.get("test.nt"), "N-Triples")    // FIXME CMEM-1759 use quad file when QuadSink is available
    private val input = Seq(new SparqlEndpointEntityTable(source.sparqlEndpoint, PlainTask("endpointTask", DatasetSpec.empty)))

    private val WORKFLOW_ID = "copy_sparql"
    private val OUTPUT_DATASET_ID = "sparql_out"
    private val INPUT_DATASET_ID = "test"

    for(withTempfile <- Seq(true, false)){
      val task = PlainTask("task", SparqlCopyCustomTask(constructQuery, withTempfile))

      it should "copy correct triples and store them optionally in a temp file " + withTempfile in {
        val result = executor.execute(task, input, ExecutorOutput.empty, execution, context)
        result match{
          case Some(copy) =>
            copy.entities.size mustBe 5                    // number of triples in source
            copy.entities.forall(e => e.values.size == 5) mustBe true   // default number of quad values as entities
          case None => fail("Empty result of copy task")
        }
      }
    }

    it should "delete temporary files via shutdown hook " in {
      execution.executeShutdownHooks()
      val tempFiles = new File(System.getProperty("java.io.tmpdir")).listFiles().toList.sortBy(f => f.lastModified())
      if(tempFiles.last.getName.contains("counstruct_copy_tmp") && tempFiles.last.exists())
        fail("Temp file was not deleted")
    }

    it should "run sparql copy workflow" in {
      val project = WorkspaceFactory().workspace.project(projectId)
      val workflowTask = project.task[Workflow](WORKFLOW_ID)
      val executor = LocalWorkflowExecutorGeneratingProvenance(workflowTask)
      val activityContext = new ActivityMonitor(name = "ReportMonitor", initialValue = Some(WorkflowExecutionReportWithProvenance.empty))
      executor.run(activityContext)
      val outputDataset = project.task[GenericDatasetSpec](OUTPUT_DATASET_ID).data.plugin
      val inputDataset = project.task[GenericDatasetSpec](INPUT_DATASET_ID).data.plugin
      val allOutputTriples = outputDataset.asInstanceOf[RdfFileDataset].sparqlEndpoint.constructModel("CONSTRUCT { ?s ?p ?o. } WHERE { ?s ?p ?o }")
      allOutputTriples.size() mustBe 13
      val allInputTriples = inputDataset.asInstanceOf[RdfFileDataset].sparqlEndpoint.constructModel("CONSTRUCT { ?s ?p ?o. } WHERE { ?s ?p ?o }")
      allInputTriples.isIsomorphicWith(allOutputTriples) mustBe true
    }
  }
