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
  *
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
    val entitySink = dataset.entitySink
    entitySink.openTableWithPaths("", paths, false)
    entitySink.writeEntity(
      subject,
      IndexedSeq(entities)
    )
    entitySink.closeTable()
    entitySink.close()
    val result = dataset.sparqlEndpoint.select(s"SELECT ?s ?o WHERE { ?s $propertyUri ?o } ORDER BY ?s").bindings.toSeq
    result.flatMap(_.get("s")) mustBe entities.map(Resource)
    result.flatMap(_.get("o")) mustBe Seq(Resource(subject), Resource(subject))
  }

  it should "share the size counter across separate sparqlEndpoint accesses (app-scoped)" in {
    ConfigTestTrait.withConfig(RuntimeResource.maxInMemorySizeParameterName -> Some("50b")) {
      val dataset = InMemoryDataset(workflowScoped = false)

      // First access writes 26 estimated bytes (within the 50b limit)
      dataset.sparqlEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")

      // A fresh sparqlEndpoint call must see the previously written data and trip the limit
      an[RuntimeException] should be thrownBy {
        dataset.sparqlEndpoint.update("INSERT DATA { <http://s2> <http://p> <http://o2> }")
      }
    }
  }

  it should "share the size counter between the executor and the dataset (workflow-scoped)" in {
    ConfigTestTrait.withConfig(RuntimeResource.maxInMemorySizeParameterName -> Some("50b")) {
      val dataset = InMemoryDataset(workflowScoped = true)
      val task = PlainTask("sizeLimitTest", DatasetSpec(dataset))
      val executor = new InMemoryDatasetExecutor()

      val executorEndpoint = sparqlEndpoint(executor.access(task, LocalExecution()))
      executorEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")

      // Writing through the dataset's own sparqlEndpoint after the executor must hit the same counter
      an[RuntimeException] should be thrownBy {
        dataset.sparqlEndpoint.update("INSERT DATA { <http://s2> <http://p> <http://o2> }")
      }
    }
  }

  private def sparqlEndpoint(access: DatasetAccess): SparqlEndpoint =
    access.asInstanceOf[RdfDatasetAccess].sparqlEndpoint
}
