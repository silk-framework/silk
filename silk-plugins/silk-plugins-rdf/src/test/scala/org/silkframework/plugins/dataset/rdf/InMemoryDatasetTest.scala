package org.silkframework.plugins.dataset.rdf

import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.rdf.{RdfDatasetAccess, Resource, SparqlEndpoint}
import org.silkframework.dataset.{DatasetAccess, DatasetSpec}
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.local.LocalExecution
import org.silkframework.plugins.dataset.rdf.datasets.{InMemoryDataset, InMemoryDatasetExecutor}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{Resource => RuntimeResource}
import org.silkframework.util.{ConfigTestTrait, MockitoSugar}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/**
  * Tests the in-memory dataset.
  */
class InMemoryDatasetTest extends AnyFlatSpec with Matchers with MockitoSugar {

  private implicit val userContext: UserContext = UserContext.Empty
  private implicit val prefixes: Prefixes = Prefixes.empty

  behavior of "In-memory Dataset"

  it should "write correct RDF when given entities with backward paths" in {
    val dataset = InMemoryDataset()
    val propertyUri = "<http://domain.com/backwardPath>"
    val subject = "http://subject.com/uri"
    val entities = Seq("http://domain.com/entity/1", "http://domain.com/entity/2")
    val paths = IndexedSeq(
      TypedPath(UntypedPath.parse(s"\\$propertyUri"), ValueType.URI, isAttribute = false)
    )
    val task = PlainTask("inMemoryWrite", DatasetSpec(dataset))
    val access = new InMemoryDatasetExecutor().access(task, LocalExecution())
    val entitySink = access.entitySink
    entitySink.openTableWithPaths("", paths, false)
    entitySink.writeEntity(
      subject,
      IndexedSeq(entities)
    )
    entitySink.closeTable()
    entitySink.close()
    val result = sparqlEndpoint(access).select(s"SELECT ?s ?o WHERE { ?s $propertyUri ?o } ORDER BY ?s").bindings.toSeq
    result.flatMap(_.get("s")) mustBe entities.map(Resource)
    result.flatMap(_.get("o")) mustBe Seq(Resource(subject), Resource(subject))
  }

  it should "share the size counter across separate executor accesses (app-scoped)" in {
    ConfigTestTrait.withConfig(RuntimeResource.maxInMemorySizeParameterName -> Some("50b")) {
      val dataset = InMemoryDataset(workflowScoped = false)
      val task = PlainTask("sizeLimitTest", DatasetSpec(dataset))

      // First access writes 26 estimated bytes (within the 50b limit)
      sparqlEndpoint(new InMemoryDatasetExecutor().access(task, LocalExecution()))
        .update("INSERT DATA { <http://s1> <http://p> <http://o1> }")

      // A fresh executor access must see the previously written data and trip the limit
      an[RuntimeException] should be thrownBy {
        sparqlEndpoint(new InMemoryDatasetExecutor().access(task, LocalExecution()))
          .update("INSERT DATA { <http://s2> <http://p> <http://o2> }")
      }
    }
  }

  it should "share the size counter between independent executor accesses (workflow-scoped)" in {
    ConfigTestTrait.withConfig(RuntimeResource.maxInMemorySizeParameterName -> Some("50b")) {
      val dataset = InMemoryDataset(workflowScoped = true)
      val task = PlainTask("sizeLimitTest", DatasetSpec(dataset))
      val executor = new InMemoryDatasetExecutor()

      val executorEndpoint = sparqlEndpoint(executor.access(task, LocalExecution()))
      executorEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")

      // Writing through a second, independent executor access must hit the same counter
      an[RuntimeException] should be thrownBy {
        sparqlEndpoint(new InMemoryDatasetExecutor().access(task, LocalExecution()))
          .update("INSERT DATA { <http://s2> <http://p> <http://o2> }")
      }
    }
  }

  private def sparqlEndpoint(access: DatasetAccess): SparqlEndpoint =
    access.asInstanceOf[RdfDatasetAccess].sparqlEndpoint
}
