package org.silkframework.plugins.dataset.rdf.sparql

import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.rdf.SparqlPathBuilder
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.testutil.equalIgnoringWhitespace
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SparqlPathBuilderTest extends AnyFlatSpec with Matchers {

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

  it should "check for special paths and generate the correct query" in {
    for(path <- Seq(
      "?a/<urn:prop:PropA>/#text/propB",
      "?a/<urn:prop:PropA>/#text\\propB",
      "?a/<urn:prop:PropA>\\#text",
      "?a/<urn:prop:PropA>/#lang/propB",
      "?a/<urn:prop:PropA>/#lang\\propB",
      "?a/<urn:prop:PropA>\\#lang"
    )) {
      intercept[ValidationException] {
        build(path)
      }
    }
    build(s"?a/<urn:prop:PropA>/#lang") should be(equalIgnoringWhitespace("OPTIONAL { ?s <urn:prop:PropA> ?v0 . }"))
    build(s"?a/<urn:prop:PropA>/#text") should be(equalIgnoringWhitespace("OPTIONAL { ?s <urn:prop:PropA> ?v0 . }"))
  }

  def build(path: String, useOptional: Boolean = true): String = SparqlPathBuilder(Seq(UntypedPath.parse(path)), useOptional = useOptional)

}
