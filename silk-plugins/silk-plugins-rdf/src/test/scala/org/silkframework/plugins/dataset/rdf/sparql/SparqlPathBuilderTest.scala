package org.silkframework.plugins.dataset.rdf.sparql

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.rdf.SparqlPathBuilder
import org.silkframework.testutil.equalIgnoringWhitespace

class SparqlPathBuilderTest extends FlatSpec with Matchers {

  // Example properties
  val p1 = "<1>"
  val p2 = "<2>"

  "SparqlPathBuilder" should "build SPARQL patterns for simple paths" in {
    build(s"?a/$p1") should be(equalIgnoringWhitespace(s"OPTIONAL { ?s $p1 ?v0 . }"))
    build(s"?a\\$p1") should be(equalIgnoringWhitespace(s"OPTIONAL { ?v0 $p1 ?s . }"))
  }

  it should "build SPARQL patterns without OPTIONALs" in {
    build(s"?a/$p1", useOptional = false) should be(equalIgnoringWhitespace(s"?s $p1 ?v0 ."))
    build(s"?a\\$p1", useOptional = false) should be(equalIgnoringWhitespace(s"?v0 $p1 ?s ."))
  }

  it should "include Filter statements" in {
    build(s"?a/<1>[<2> = <3>]") should be(equalIgnoringWhitespace("OPTIONAL { ?s <1> ?v0 . ?v0 <2> ?f1 . FILTER(?f1 = <3>). }"))
  }

  def build(path: String, useOptional: Boolean = true): String = SparqlPathBuilder(Seq(UntypedPath.parse(path)), useOptional = useOptional)

}
