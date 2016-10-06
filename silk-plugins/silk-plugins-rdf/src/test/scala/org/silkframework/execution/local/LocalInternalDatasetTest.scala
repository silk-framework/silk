package org.silkframework.execution.local

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.plugins.dataset.InternalDatasetTrait
import org.silkframework.util.ConfigTestTrait

/**
  * Tests the internal dataset. This test is located in this module as the default is to use the RDF-based one from this module.
  */
class LocalInternalDatasetTest extends FlatSpec with MustMatchers with ConfigTestTrait {
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
      sink.writeTriple("s" + id.getOrElse("None"), "b", "o")
      sink.close()
      ds.sparqlEndpoint.select("SELECT ?s WHERE {?s ?p ?o}").bindings.size mustBe 1
    }
  }

  /** The properties that should be changed.
    * If the value is [[None]] then the property value is removed,
    * else it is set to the new value.
    */
  override def propertyMap: Map[String, Option[String]] = Map(
    "dataset.internal.plugin" -> Some("inMemory")
  )
}
