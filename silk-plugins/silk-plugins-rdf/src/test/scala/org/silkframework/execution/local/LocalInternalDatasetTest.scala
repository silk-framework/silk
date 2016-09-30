package org.silkframework.execution.local

import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Tests the internal dataset. This test is located in this module as the default is to use the RDF-based one from this module.
  */
class LocalInternalDatasetTest extends FlatSpec with MustMatchers {
  "LocalInternalDataset" should "store and retrieve data" in {
    val ds = LocalInternalDataset()
    val sink = ds.tripleSink
    sink.init()
    sink.writeTriple("s", "b", "o")
    sink.close()
    ds.sparqlEndpoint.select("SELECT ?s WHERE {?s ?p ?o}").bindings.size mustBe 1
  }
}