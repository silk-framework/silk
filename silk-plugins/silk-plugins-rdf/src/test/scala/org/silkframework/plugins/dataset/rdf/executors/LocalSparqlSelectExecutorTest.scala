package org.silkframework.plugins.dataset.rdf.executors

import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.rdf.{PlainLiteral, RdfNode, Resource, SparqlEndpoint, SparqlEndpointEntityTable, SparqlParams, SparqlResults}
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.runtime.activity.{TestUserContextTrait, UserContext}
import org.silkframework.runtime.plugin.MultilineStringParameter

import scala.collection.immutable.SortedMap

class LocalSparqlSelectExecutorTest extends FlatSpec with MustMatchers with TestUserContextTrait with MockitoSugar {
  behavior of "Local SPARQL Select executor"

  it should "not run out of memory and fetch first entity immediately on large result sets" in {
    val quickReactionTime = 500 // quick in the sense that it won't take too long even on a heavy-loaded CI system
    val task = SparqlSelectCustomTask(MultilineStringParameter("SELECT * WHERE {?s ?p ?o}"))
    val limit = 1000 * 1000
    val sparqlEndpoint = new SparqlEndpoint {
      override def sparqlParams: SparqlParams = ???
      override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = ???
      override def select(query: String, limit: Int)(implicit userContext: UserContext): SparqlResults = {
        SparqlResults(new Traversable[SortedMap[String, RdfNode]] {
          override def foreach[U](f: SortedMap[String, RdfNode] => U): Unit = {
            var i = 0
            while(i < limit) {
              f(SortedMap("s" -> Resource(s"subject $i"), "p" -> Resource(s"predicate $i"), "o" -> PlainLiteral(s"literal $i")))
              i += 1
            }
          }
        })
      }
    }
    val entityTable = new SparqlEndpointEntityTable(sparqlEndpoint, mock[Task[TaskSpec]])
    val start = System.currentTimeMillis()
    val entities = LocalSparqlSelectExecutor().executeOnSparqlEndpointEntityTable(task, entityTable)
    entities.head.values.flatten.head mustBe "subject 0"
    System.currentTimeMillis() - start mustBe < (quickReactionTime)
  }
}
