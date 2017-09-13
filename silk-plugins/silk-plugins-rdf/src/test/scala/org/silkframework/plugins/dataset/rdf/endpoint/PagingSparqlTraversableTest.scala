package org.silkframework.plugins.dataset.rdf.endpoint

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.dataset.rdf._

import scala.xml.Elem

/**
  * Paging SPARQL Traversable tests
  */
class PagingSparqlTraversableTest extends FlatSpec with MustMatchers {
  behavior of "Paging SPARQL Traversable"
  private final val URI = "uri"
  private final val BNODE = "bNode"
  private final val PLAIN_LITERAL = "plainLiteral"
  private final val LANG_LITERAL = "languageLiteral"
  private final val DATA_TYPE_LITERAL = "dataTypeLiteral"

  private val sparqlResults: Elem = {
    <sparql xmlns="http://www.w3.org/2005/sparql-results#">
      <head>
        <variable name={URI}/>
        <variable name={BNODE}/>
        <variable name={PLAIN_LITERAL}/>
        <variable name={LANG_LITERAL}/>
        <variable name={DATA_TYPE_LITERAL}/>
      </head>

      <results>
        <result>
          <binding name={URI}>
            <uri>http://this.is.a.uri.com</uri>
          </binding>
          <binding name={BNODE}>
            <bnode>bNode</bnode>
          </binding>
          <binding name={PLAIN_LITERAL}>
            <literal>plain</literal>
          </binding>
          <binding name={LANG_LITERAL}>
            <literal xml:lang="en">in English</literal>
          </binding>
          <binding name={DATA_TYPE_LITERAL}>
            <literal datatype="http://www.w3.org/2001/XMLSchema#integer">42</literal>
          </binding>
        </result>
      </results>
    </sparql>
  }

  it should "return correctly typed query results" in {
    val results = PagingSparqlTraversable("", _ => sparqlResults, SparqlParams(), 10)
    val result = results.bindings.head
    result(URI) mustBe Resource("http://this.is.a.uri.com")
    result(BNODE) mustBe a[BlankNode]
    result(PLAIN_LITERAL) mustBe PlainLiteral("plain")
    result(LANG_LITERAL) mustBe LanguageLiteral("in English", "en")
    result(DATA_TYPE_LITERAL) mustBe DataTypeLiteral("42", "http://www.w3.org/2001/XMLSchema#integer")
  }
}
