package org.silkframework.plugins.dataset.rdf

  import java.io.File

  import org.scalatest.mock.MockitoSugar
  import org.scalatest.{FlatSpec, MustMatchers}
  import org.silkframework.config.PlainTask
  import org.silkframework.dataset.DatasetSpec
  import org.silkframework.dataset.rdf.SparqlEndpointEntityTable
  import org.silkframework.execution.ExecutionReport
  import org.silkframework.execution.local.LocalExecution
  import org.silkframework.plugins.dataset.rdf.datasets.RdfFileDataset
  import org.silkframework.plugins.dataset.rdf.executors.LocalSparqlCopyExecutor
  import org.silkframework.plugins.dataset.rdf.tasks.SparqlCopyCustomTask
  import org.silkframework.runtime.activity.{ActivityContext, UserContext}
  import org.silkframework.runtime.resource.{ClasspathResourceLoader, FileResource, ReadOnlyResourceManager}

  class LocalSparqlCopyOperatorTest extends FlatSpec with MustMatchers with MockitoSugar {
    behavior of "Local SPARQL Copy Executor"

    implicit val uc: UserContext = UserContext.Empty

    private lazy val resources = ReadOnlyResourceManager(ClasspathResourceLoader(getClass.getPackage.getName.replace('.', '/')))

    // execution data
    private val execution = LocalExecution(true)
    private val executor = new LocalSparqlCopyExecutor()
    private val constructQuery = "CONSTRUCT { ?s ?p ?o. } WHERE { ?s ?p ?o. FILTER(?s = <http://dbpedia.org/resource/Albert_Einstein>) }"
    private val context = mock[ActivityContext[ExecutionReport]]
    private val source = RdfFileDataset(resources.get("test.nt"), "N-Triples")    // FIXME use quad file when QuadSink is available
    private val input = Seq(new SparqlEndpointEntityTable(source.sparqlEndpoint, PlainTask("endpointTask", DatasetSpec.empty)))

    private var tempFile: File = _

    for(withTempfile <- Seq(true, false)){
      val task = PlainTask("task", SparqlCopyCustomTask(constructQuery, withTempfile))

      it should "copy correct triples and store them optionally in a temp file " + withTempfile in {
        val result = executor.execute(task, input, None, execution, context)
        result match{
          case Some(copy) =>
            if(withTempfile) {
              tempFile = copy.task.data.asInstanceOf[DatasetSpec[RdfFileDataset]].plugin.file.asInstanceOf[FileResource].file
              tempFile.exists() mustBe true
            }
            copy.entities.size mustBe 5                    // number of triples in source
            copy.entities.forall(e => e.values.size == 4)   // we are dealing with triples (+ valueType)
          case None => fail("Empty result of copy task")
        }
      }

      it should "delete temporary files via shutdown hook " + withTempfile in {
        if(withTempfile){
          execution.executeShutdownHooks()
          tempFile.exists() mustBe false
        }
      }
    }
  }
