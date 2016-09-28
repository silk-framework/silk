package org.silkframework.execution.local

import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created on 9/2/16.
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
