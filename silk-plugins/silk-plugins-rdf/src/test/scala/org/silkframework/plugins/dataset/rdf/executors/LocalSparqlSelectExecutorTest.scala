package org.silkframework.plugins.dataset.rdf.executors


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset.rdf._
import org.silkframework.dataset.{DataSource, DatasetSpec, EntitySink, LinkSink}
import org.silkframework.entity.Entity
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.runtime.activity.{TestUserContextTrait, UserContext}
import org.silkframework.runtime.iterator.{CloseableIterator, TraversableIterator}
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext}
import org.silkframework.util.{MockitoSugar, TestMocks}

import scala.collection.immutable.SortedMap

class LocalSparqlSelectExecutorTest extends AnyFlatSpec
    with Matchers
    with TestUserContextTrait
    with MockitoSugar {
  behavior of "Local SPARQL Select executor"

  val timeout = 50
  implicit val pluginContext: PluginContext = PluginContext.empty

  it should "not run out of memory and fetch first entity immediately on large result sets" in {
    val quickReactionTime = 500 // quick in the sense that it won't take too long even on a heavy-loaded CI system
    val activityContextMock = TestMocks.activityContextMock()
    val task = SparqlSelectCustomTask("SELECT * WHERE {?s ?p ?o}")
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), activityContextMock)
    val sparqlEndpoint = new SparqlEndpoint {
      override def sparqlParams: SparqlParams = SparqlParams()
      override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = this
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
    val entities = new LocalSparqlSelectIterator(task, taskWithEndpoint(sparqlEndpoint), None, executionReportUpdater = Some(reportUpdater))
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
    val entities = new LocalSparqlSelectIterator(task, taskWithEndpoint(sparqlEndpoint), None, limit, Some(reportUpdater))
    entities.headOption // Needed to actually execute the query
    correctTimeout mustBe true
  }

  it should "evaluate a Jinja query template using the graph variable from the task parameters" in {
    val graphUri = "http://example.org/testGraph"
    val query = """SELECT * WHERE { GRAPH {{ inputProperties.uri("graph") }} { ?s ?p ?o } }"""
    val task = SparqlSelectCustomTask(query)
    var capturedQuery = ""
    val activityContextMock = TestMocks.activityContextMock()
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), activityContextMock)
    val sparqlEndpoint = sparqlEndpointStub(queryCapture = q => capturedQuery = q)
    LocalSparqlSelectExecutor().executeOnSparqlEndpoint(task, taskWithEndpoint(sparqlEndpoint, graphUri = Some(graphUri)), None, executionReportUpdater = Some(reportUpdater)).headOption

    task.outputSchema.typedPaths.map(_.toUntypedPath.normalizedSerialization) mustBe IndexedSeq("s", "p", "o")
    capturedQuery must include(s"<$graphUri>")
    capturedQuery must not include "inputProperties.uri"
  }

  private def taskWithEndpoint(sparqlEndpoint: SparqlEndpoint, graphUri: Option[String] = None): Task[DatasetSpec[RdfDataset]] = {
    PlainTask("testDataset", DatasetSpec(new StubRdfDataset(sparqlEndpoint, graphUri)))
  }

  private class StubRdfDataset(endpoint: SparqlEndpoint, graphUri: Option[String] = None) extends RdfDataset {
    override def sparqlEndpoint: SparqlEndpoint = endpoint
    override def parameters(implicit pluginContext: PluginContext): ParameterValues = {
      graphUri match {
        case Some(g) => ParameterValues.fromStringMap(Map("graph" -> g))
        case None => ParameterValues.empty
      }
    }
    override def source(implicit userContext: UserContext): DataSource = ???
    override def linkSink(implicit userContext: UserContext): LinkSink = ???
    override def entitySink(implicit userContext: UserContext): EntitySink = ???
  }

  private def sparqlEndpointStub(selectCallback: SparqlEndpoint => Unit = _ => {},
                                 graphUri: Option[String] = None,
                                 queryCapture: String => Unit = _ => {}): SparqlEndpoint = {
    new SparqlEndpoint {
      var sparqlParamsIntern = SparqlParams(graph = graphUri)
      override def sparqlParams: SparqlParams = sparqlParamsIntern

      override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = {
        sparqlParamsIntern = sparqlParams // This is not immutable, but is OK for the test
        this
      }

      override def select(query: String, limit: Int)(implicit userContext: UserContext): SparqlResults = {
        selectCallback(this)
        queryCapture(query)
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