package org.silkframework.plugins.dataset.rdf


import org.silkframework.config.Prefixes
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/** Integration tests for the SPARQL Update operator improvements */
class SparqlUpdateTaskExtensionIntegrationTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait {
  behavior of "SPARQL Update Task extensions"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  implicit val prefixes: Prefixes = Prefixes.empty

  it should "not read from data source when defining a static SPARQL Update template" in {
    executeWorkflow("staticSparqlUpdateWorkflow")
    /*
    Workflow description:
    1. Invalid Knowledge Graph (which fails when read)
    2. SPARQL Update operator with static template
    3. In-memory RDF output
     */
    val datasetTask = project.task[GenericDatasetSpec]("outputRdf")
    val dataset = datasetTask.data
    val paths = dataset.source.retrievePaths("")
    paths.size mustBe 1
    paths.head.normalizedSerialization mustBe "<urn:prop:1>"
  }

  override def projectPathInClasspath: String = "org/silkframework/plugins/dataset/rdf/improveSparqlUpdateProject.zip"

}
