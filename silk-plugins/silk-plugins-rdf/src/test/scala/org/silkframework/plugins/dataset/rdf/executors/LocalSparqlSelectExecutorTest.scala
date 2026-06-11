package org.silkframework.plugins.dataset.rdf.executors


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.{DatasetAccess, DatasetSpec}
import org.silkframework.dataset.rdf._
import org.silkframework.entity.Entity
import org.silkframework.execution.ExecutorOutput
import org.silkframework.execution.local.LocalExecution
import org.silkframework.execution.typed.SparqlEndpointEntitySchema
import org.silkframework.plugins.dataset.rdf.datasets.{InMemoryDataset, InMemoryDatasetExecutor}
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.runtime.activity.{TestUserContextTrait, UserContext}
import org.silkframework.runtime.iterator.{CloseableIterator, TraversableIterator}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.{Identifier, MockitoSugar, TestMocks}

import scala.collection.immutable.SortedMap

class LocalSparqlSelectExecutorTest extends AnyFlatSpec
    with Matchers
    with TestUserContextTrait
    with MockitoSugar {
  behavior of "Local SPARQL Select executor"

  val timeout = 50
  implicit val pluginContext: PluginContext = PluginContext.empty
  implicit val prefixes: Prefixes = Prefixes.empty

  private val tripleCountQuery = "SELECT * WHERE {?s ?p ?o}"

  it should "not run out of memory and fetch first entity immediately on large result sets" in {
    val quickReactionTime = 500 // quick in the sense that it won't take too long even on a heavy-loaded CI system
    val activityContextMock = TestMocks.activityContextMock()
    val task = SparqlSelectCustomTask("SELECT * WHERE {?s ?p ?o}")
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), activityContextMock)
    val sparqlEndpoint = new SparqlEndpoint {
      override def sparqlParams: SparqlParams = ???
      override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = ???
      override def select(query: String, limit: Int)(implicit userContext: UserContext): SparqlResults = {
        val entities =
          for(i <- Iterator.range(0, limit)) yield {
            SortedMap("s" -> Resource(s"subject $i"), "p" -> Resource(s"predicate $i"), "o" -> PlainLiteral(s"literal $i"))
          }
        SparqlResults(Seq("s", "p", "o"), CloseableIterator(entities))
      }
      override def ask(query: String)(implicit userContext: UserContext): SparqlAskResult = ???
    }
    Entity.empty("") // Make sure that Entity class is loaded
    val start = System.currentTimeMillis()
    val entities = new LocalSparqlSelectIterator(task, sparqlEndpoint, executionReportUpdater = Some(reportUpdater))
    val entity = entities.head
    entity.values.flatten.head mustBe "subject 0"
    (System.currentTimeMillis() - start).toInt must be < quickReactionTime
  }


  it should "pass query timeout to SPARQL endpoint if a query timeout is configured" in {
    val task = SparqlSelectCustomTask("SELECT * WHERE {?s ?p ?o}", sparqlTimeout = timeout)
    var correctTimeout = false
    val activityContextMock = TestMocks.activityContextMock()
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), activityContextMock)
    val sparqlEndpoint = sparqlEndpointStub(selectCallback = endpoint => {
      correctTimeout = endpoint.sparqlParams.timeout.contains(timeout)
    })
    val limit = 1000 * 1000 * 1000
    val entities = new LocalSparqlSelectIterator(task, sparqlEndpoint, limit, Some(reportUpdater))
    entities.headOption // Needed to actually execute the query
    correctTimeout mustBe true
  }

  it should "read from the execution-scoped dataset access, not the shared dataset endpoint" in {
    // Regression: two instances of the same workflow run in parallel against the same workflow-scoped
    // InMemoryDataset task. The select executor must read the model of its own execution, not the
    // shared (and concurrently overwritten) dataset.sparqlEndpoint reference.
    val dataset = InMemoryDataset(workflowScoped = true)
    val task = PlainTask("inMemorySource", DatasetSpec(dataset))

    val execution1 = LocalExecution(false, workflowId = Some(Identifier("wf1")))
    val execution2 = LocalExecution(false, workflowId = Some(Identifier("wf2")))

    // Keep the writer executors referenced for the whole test: the dataset stores its per-execution
    // endpoints in a WeakHashMap keyed by a key that only the owning executor holds, so a discarded
    // executor would let GC drop the execution's model.
    val writer1 = new InMemoryDatasetExecutor()
    val writer2 = new InMemoryDatasetExecutor()

    // Write one triple for execution1 and two for execution2 through the execution-scoped access path.
    sparqlEndpoint(writer1.access(task, execution1))
      .update("INSERT DATA { <http://s1> <http://p> <http://o1> }")
    sparqlEndpoint(writer2.access(task, execution2))
      .update("INSERT DATA { <http://s2> <http://p> <http://o2> . <http://s3> <http://p> <http://o3> }")

    // execution2 accessed last, so the shared dataset endpoint now points at execution2's model.
    // This is the value the executor would (wrongly) return if it read the dataset endpoint directly.
    dataset.sparqlEndpoint.select(tripleCountQuery).bindings.size mustBe 2

    val selectTask = PlainTask("select", SparqlSelectCustomTask(tripleCountQuery))
    val input = Seq(SparqlEndpointEntitySchema.create(task))
    val executor = LocalSparqlSelectExecutor()

    def rowCount(execution: LocalExecution): Int = {
      val result = executor.execute(selectTask, input, ExecutorOutput.empty, execution, TestMocks.activityContextMock())
      result.getOrElse(fail("SPARQL select executor returned no result")).entities.size
    }

    // Each execution must see only its own model.
    rowCount(execution1) mustBe 1
    rowCount(execution2) mustBe 2

    // Keeps the writer executors (and thus their endpoint keys) alive until here, and cleans up.
    writer1.close()
    writer2.close()
  }

  private def sparqlEndpoint(access: DatasetAccess): SparqlEndpoint =
    access.asInstanceOf[RdfDatasetAccess].sparqlEndpoint

  private def sparqlEndpointStub(selectCallback: SparqlEndpoint => Unit = _ => {}): SparqlEndpoint = {
    new SparqlEndpoint {
      var sparqlParamsIntern = SparqlParams()
      override def sparqlParams: SparqlParams = sparqlParamsIntern

      override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = {
        sparqlParamsIntern = sparqlParams // This is not immutable, but is OK for the test
        this
      }

      override def select(query: String, limit: Int)(implicit userContext: UserContext): SparqlResults = {
        selectCallback(this)
        SparqlResults(Seq("s", "p", "o"), new TraversableIterator[SortedMap[String, RdfNode]] {
          override def foreach[U](f: SortedMap[String, RdfNode] => U): Unit = {
            var i = 0
            while (i < limit) {
              f(SortedMap("s" -> Resource(s"subject $i"), "p" -> Resource(s"predicate $i"), "o" -> PlainLiteral(s"literal $i")))
              i += 1
            }
          }
        })
      }
    }
  }
}
