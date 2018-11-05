package org.silkframework.plugins.dataset.rdf.endpoint

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.dataset.rdf._

import scala.collection.mutable.ArrayBuffer

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

  private def sparqlResults(nrResults: Int): InputStream = {
    val result = <sparql xmlns="http://www.w3.org/2005/sparql-results#">
      <head>
        <variable name={URI}/>
        <variable name={BNODE}/>
        <variable name={PLAIN_LITERAL}/>
        <variable name={LANG_LITERAL}/>
        <variable name={DATA_TYPE_LITERAL}/>
      </head>

      <results>
        { for(_ <- 1 to nrResults) yield {
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
      }}
      </results>
    </sparql>
    new ByteArrayInputStream(("<?xml version=\"1.0\"?>\n" + result.toString()).getBytes(StandardCharsets.UTF_8))
  }

  it should "return correctly typed query results" in {
    val results = PagingSparqlTraversable("select * where {?s ?p ?o}", _ => sparqlResults(2), SparqlParams(), Int.MaxValue).bindings.toArray
    val result = results.head
    result(URI) mustBe Resource("http://this.is.a.uri.com")
    result(BNODE) mustBe a[BlankNode]
    result(PLAIN_LITERAL) mustBe PlainLiteral("plain")
    result(LANG_LITERAL) mustBe LanguageLiteral("in English", "en")
    result(DATA_TYPE_LITERAL) mustBe DataTypeLiteral("42", "http://www.w3.org/2001/XMLSchema#integer")
    results.length mustBe 2
  }

  it should "handle graph parameter" in {
    val queries = ArrayBuffer[String]()
    val queryCollector: String => InputStream = { query =>
      queries.append(query)
      sparqlResults(1) // just a dummy
    }
    val GRAPH_URI = "http://graph.com/graph"
    val sparqlParams = SparqlParams(graph = Some(GRAPH_URI), pageSize = 1)
    val result = PagingSparqlTraversable("SELECT * WHERE { ?s ?p ?o }", queryCollector, sparqlParams, Int.MaxValue)
    val results = result.bindings.take(3).toArray
    val requestedQueries = queries.toList
    requestedQueries.size mustBe 3
    for(i <- 0 to 2) {
      requestedQueries(i).contains(s"OFFSET $i LIMIT 1")
    }
    queries.head.contains(s"FROM <$GRAPH_URI>")
  }

  it should "not set limit and offset if already in query" in {
    val queries = ArrayBuffer[String]()
    val queryCollector: String => InputStream = { query =>
      queries.append(query)
      sparqlResults(1) // just a dummy
    }
    val sparqlParams = SparqlParams(pageSize = 1)
    val result = PagingSparqlTraversable("SELECT * WHERE { ?s ?p ?o } LIMIT 1000", queryCollector, sparqlParams, Int.MaxValue)
    result.bindings.take(3).toArray
    val requestedQueries = queries.toList
    requestedQueries.size mustBe 1
    queries.head.split("\\s+") must not contain oneOf("FROM", "NAMED", "OFFSET")
    queries.head.contains("1000") mustBe true
  }

  it should "reduce the page size to the limit if the page size if greater" in {
    val lowLimit = 42
    val queries = ArrayBuffer[String]()
    val queryCollector: String => InputStream = { query =>
      queries.append(query)
      sparqlResults(1) // just a dummy
    }
    val sparqlParams = SparqlParams(pageSize = 1000000)
    val result = PagingSparqlTraversable("SELECT * WHERE { ?s ?p ?o }", queryCollector, sparqlParams, lowLimit)
    result.bindings.head // execute
    queries must have size 1
    queries.head.contains(lowLimit) mustBe true
  }

  it should "stop paging when the thread is interrupted" in {
    val sparqlParams = SparqlParams(pageSize = 1)
    val infiniteLoop: String => InputStream = { _ =>
      sparqlResults(1)
    }
    val count = new AtomicInteger(0)
    val thread = new Thread {
      override def run(): Unit = {
        val result = PagingSparqlTraversable("SELECT * WHERE { ?s ?p ?o }", infiniteLoop, sparqlParams, limit = Int.MaxValue)
        for(_ <- result.bindings) {
          count.incrementAndGet()
        }
      }
    }
    thread.start()
    val start = System.currentTimeMillis()
    while(thread.isAlive) {
      if(count.get() > 10) {
        thread.interrupt()
      }
    }
    System.currentTimeMillis() - start must be < 5 * 1000L // If it is not interrupted this should run for minutes
  }
}
