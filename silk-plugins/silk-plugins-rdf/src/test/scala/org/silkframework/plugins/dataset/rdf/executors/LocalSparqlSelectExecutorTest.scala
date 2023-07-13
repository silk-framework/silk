package org.silkframework.plugins.dataset.rdf.executors


import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.dataset.rdf._
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.runtime.activity.{TestUserContextTrait, UserContext}
import org.silkframework.util.{MockitoSugar, TestMocks}

import scala.collection.immutable.SortedMap
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.entity.Entity
import org.silkframework.runtime.iterator.{CloseableIterator, TraversableIterator}
import org.silkframework.runtime.iterator.TraversableIterator
import org.silkframework.runtime.plugin.types.MultilineStringParameter

class LocalSparqlSelectExecutorTest extends AnyFlatSpec
    with Matchers
    with TestUserContextTrait
    with MockitoSugar {
  behavior of "Local SPARQL Select executor"

  val timeout = 50

  it should "not run out of memory and fetch first entity immediately on large result sets" in {
    val quickReactionTime = 500 // quick in the sense that it won't take too long even on a heavy-loaded CI system
    val activityContextMock = TestMocks.activityContextMock()
    val task = SparqlSelectCustomTask(MultilineStringParameter("SELECT * WHERE {?s ?p ?o}"))
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
    val activityContextMock = TestMocks.activityContextMock()
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), activityContextMock)
    val sparqlEndpoint = sparqlEndpointStub(selectCallback = endpoint => {
      correctTimeout = endpoint.sparqlParams.timeout.contains(timeout)
    })
    val entityTable = new SparqlEndpointEntityTable(sparqlEndpoint, mock[Task[TaskSpec]])
    val limit = 1000 * 1000 * 1000
    val entities = LocalSparqlSelectExecutor().executeOnSparqlEndpointEntityTable(task, entityTable, limit, Some(reportUpdater))
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
