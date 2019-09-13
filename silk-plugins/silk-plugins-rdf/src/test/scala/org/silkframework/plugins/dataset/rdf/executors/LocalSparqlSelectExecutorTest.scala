package org.silkframework.plugins.dataset.rdf.executors

import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.rdf._
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.runtime.activity.{TestUserContextTrait, UserContext}
import org.silkframework.runtime.plugin.MultilineStringParameter
import org.silkframework.util.TestMocks

import scala.collection.immutable.SortedMap

class LocalSparqlSelectExecutorTest extends FlatSpec
    with MustMatchers
    with TestUserContextTrait
    with MockitoSugar {
  behavior of "Local SPARQL Select executor"

  val timeout = 50

  it should "not run out of memory and fetch first entity immediately on large result sets" in {
    val quickReactionTime = 500 // quick in the sense that it won't take too long even on a heavy-loaded CI system
    val activityContextMock = TestMocks.activityContextMock()
    val reportUpdater = SparqlSelectExecutionReportUpdater("task", activityContextMock)
    val quickReactionTime = 2000 // quick in the sense that it won't take too long even on a heavy-loaded CI system
    val task = SparqlSelectCustomTask(MultilineStringParameter("SELECT * WHERE {?s ?p ?o}"))
    val sparqlEndpoint = new SparqlEndpoint {
      override def sparqlParams: SparqlParams = ???
      override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = ???
      override def select(query: String, limit: Int)(implicit userContext: UserContext): SparqlResults = {
        selectCallback(this)
        SparqlResults(new Traversable[SortedMap[String, RdfNode]] {
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
    val entityTable = new SparqlEndpointEntityTable(sparqlEndpoint, mock[Task[TaskSpec]])
    val start = System.currentTimeMillis()
    val entities = LocalSparqlSelectExecutor().executeOnSparqlEndpointEntityTable(task, entityTable, executionReportUpdater = Some(reportUpdater))
    val entity = entities.head
    entity.values.flatten.head mustBe "subject 0"
    (System.currentTimeMillis() - start).toInt must be < quickReactionTime
  }


  it should "pass query timeout to SPARQL endpoint if a query timeout is configured" in {
    val task = SparqlSelectCustomTask(MultilineStringParameter("SELECT * WHERE {?s ?p ?o}"), sparqlTimeout = timeout)
    var correctTimeout = false
    val sparqlEndpoint = sparqlEndpointStub(selectCallback = endpoint => {
      correctTimeout = endpoint.sparqlParams.timeout.contains(timeout)
    })
    val entityTable = new SparqlEndpointEntityTable(sparqlEndpoint, mock[Task[TaskSpec]])
    val limit = 1000 * 1000 * 1000
    val entities = LocalSparqlSelectExecutor().executeOnSparqlEndpointEntityTable(task, entityTable, limit = limit)
    entities.headOption // Needed to actually execute the query
    correctTimeout mustBe true
  }

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
        SparqlResults(new Traversable[SortedMap[String, RdfNode]] {
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
