package org.silkframework.plugins.dataset.rdf.endpoint

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.dataset.rdf._

import scala.collection.mutable.ArrayBuffer
import scala.xml.{Elem, Null}

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
    val results = PagingSparqlTraversable("select * where {?s ?p ?o}", _ => sparqlResults, SparqlParams(), Int.MaxValue)
    val result = results.bindings.head
    result(URI) mustBe Resource("http://this.is.a.uri.com")
    result(BNODE) mustBe a[BlankNode]
    result(PLAIN_LITERAL) mustBe PlainLiteral("plain")
    result(LANG_LITERAL) mustBe LanguageLiteral("in English", "en")
    result(DATA_TYPE_LITERAL) mustBe DataTypeLiteral("42", "http://www.w3.org/2001/XMLSchema#integer")
  }

  it should "handle graph parameter" in {
    val queries = ArrayBuffer[String]()
    val queryCollector: String => Elem = { query =>
      queries.append(query)
      sparqlResults // just a dummy
    }
    val GRAPH_URI = "http://graph.com/graph"
    val sparqlParams = SparqlParams(graph = Some(GRAPH_URI), pageSize = 1)
    val result = PagingSparqlTraversable("SELECT * WHERE { ?s ?p ?o }", queryCollector, sparqlParams, Int.MaxValue)
    result.bindings.take(3).toArray
    val requestedQueries = queries.toList
    requestedQueries.size mustBe 3
    for(i <- 0 to 2) {
      requestedQueries(i).contains(s"OFFSET $i LIMIT 1")
    }
    queries.head.contains(s"FROM <$GRAPH_URI>")
  }

  it should "not set limit and offset if already in query" in {
    val queries = ArrayBuffer[String]()
    val queryCollector: String => Elem = { query =>
      queries.append(query)
      sparqlResults // just a dummy
    }
    val sparqlParams = SparqlParams(pageSize = 1)
    val result = PagingSparqlTraversable("SELECT * WHERE { ?s ?p ?o } LIMIT 1000", queryCollector, sparqlParams, Int.MaxValue)
    result.bindings.take(3).toArray
    val requestedQueries = queries.toList
    requestedQueries.size mustBe 1
    queries.head.split("\\s+") must not contain oneOf("FROM", "NAMED", "OFFSET")
    queries.head.contains("1000") mustBe true
  }
}
