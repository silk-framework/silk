package org.silkframework.plugins.dataset.rdf

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.resource.InMemoryResourceManager

class FileDatasetTest extends FlatSpec with MustMatchers {
  behavior of "RDF file dataset"

  val resourceManager = InMemoryResourceManager()

  it should "not load files larger than 'max. read size'" in {
    val largeResource = resourceManager.get("largeResource")
    largeResource.writeString(getString(1000 * 1000 -1))
    val rdfDataset = FileDataset(largeResource, "Turtle", maxReadSize = 1)
    rdfDataset.source.retrieveTypes()
    largeResource.writeString(getString(1000 * 1000 + 1))
    val rdfDataset2 = FileDataset(largeResource, "Turtle", maxReadSize = 1)
    intercept[RuntimeException] {
      rdfDataset2.source.retrieveTypes()
    }
  }

  private def getString(size: Int) = {
    val sb = new StringBuilder()
    for (_ <- 1 to size) {
      sb.append("#")
    }
    sb.toString()
  }
}
