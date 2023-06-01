package org.silkframework.dataset.rdf

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class SparqlParamsTest extends AnyFlatSpec with Matchers {
  behavior of "SPARQL Params"
  
  val namespace = "https://www.eccenca.com/test"

  it should "split the entity list on whitespace" in {
    SparqlParams.splitEntityList(s"$namespace/A $namespace/B $namespace/C") mustBe Seq(s"$namespace/A", s"$namespace/B", s"$namespace/C")
    SparqlParams.splitEntityList("") mustBe Seq()
    SparqlParams.splitEntityList(null) mustBe Seq()
    SparqlParams.splitEntityList("   \t \n ") mustBe Seq()
    SparqlParams.splitEntityList(s"  \t$namespace/A $namespace/B\n$namespace/C \t$namespace/D") mustBe
        Seq(s"$namespace/A", s"$namespace/B", s"$namespace/C", s"$namespace/D")
  }
}
