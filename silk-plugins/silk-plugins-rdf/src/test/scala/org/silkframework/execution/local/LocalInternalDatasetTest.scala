package org.silkframework.execution.local

import org.silkframework.config.Prefixes
import org.silkframework.entity.ValueType
import org.silkframework.plugins.dataset.InternalDatasetTrait
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.InMemoryWorkspaceTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/**
  * Tests the internal dataset. This test is located in this module as the default is to use the RDF-based one from this module.
  */
class LocalInternalDatasetTest extends AnyFlatSpec with Matchers with InMemoryWorkspaceTestTrait {

  private implicit val userContext: UserContext = UserContext.Empty
  private implicit val prefixes: Prefixes = Prefixes.empty

  "LocalInternalDataset" should "store and retrieve data" in {
    val exec = LocalExecution(useLocalInternalDatasets = true)
    for(id <- Seq(None, Some("id"), Some("id2"))) {
      val ds = {
        val tempDs = exec.createInternalDataset(id)
        tempDs mustBe a[InternalDatasetTrait]
        tempDs.asInstanceOf[InternalDatasetTrait]
      }
      val sink = ds.tripleSink
      sink.init()
      sink.writeTriple("s" + id.getOrElse("None"), "b", "o", ValueType.STRING)
      sink.close()
      ds.sparqlEndpoint.select("SELECT ?s WHERE {?s ?p ?o}").bindings.size mustBe 1
    }
  }
}
