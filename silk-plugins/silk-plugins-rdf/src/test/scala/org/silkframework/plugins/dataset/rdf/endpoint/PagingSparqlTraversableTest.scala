package org.silkframework.plugins.dataset.rdf.endpoint

import org.apache.jena.query.{QueryFactory, Syntax}
import org.apache.jena.riot.Lang
import org.apache.jena.riot.resultset.ResultSetLang

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.{Charset, StandardCharsets}
import java.util.concurrent.atomic.AtomicInteger
import org.silkframework.dataset.rdf._
import org.silkframework.plugins.dataset.rdf.endpoint.PagingSparqlTraversable.QueryExecutor

import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/**
  * Paging SPARQL Traversable tests
  */
class PagingSparqlTraversableTest extends AnyFlatSpec with Matchers {
  behavior of "Paging SPARQL Traversable"
  private final val URI = "uri"
  private final val BNODE = "bNode"
  private final val PLAIN_LITERAL = "plainLiteral"
  private final val LANG_LITERAL = "languageLiteral"
  private final val DATA_TYPE_LITERAL = "dataTypeLiteral"
  private final val EMPTY_LITERAL = "emptyLiteral"

  private def sparqlResults(nrResults: Int) = new QueryExecutor {

    override protected val resultLang: Lang = ResultSetLang.RS_XML

    override def execute(query: String): InputStream = {
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
            <binding name={EMPTY_LITERAL}>
              <literal></literal>
            </binding>
          </result>
        }}
        </results>
      </sparql>
      new ByteArrayInputStream(("<?xml version=\"1.0\"?>\n" + result.toString()).getBytes(StandardCharsets.UTF_8))
    }
  }

  it should "return correctly typed query results" in {
    val results = PagingSparqlTraversable("select * where {?s ?p ?o}", sparqlResults(2), SparqlParams(), Int.MaxValue).bindings.toArray
    val result = results.head
    result(URI) mustBe Resource("http://this.is.a.uri.com")
    result(BNODE) mustBe a[BlankNode]
    result(PLAIN_LITERAL) mustBe PlainLiteral("plain")
    result(LANG_LITERAL) mustBe LanguageLiteral("in English", "en")
    result(DATA_TYPE_LITERAL) mustBe DataTypeLiteral("42", "http://www.w3.org/2001/XMLSchema#integer")
    result(EMPTY_LITERAL) mustBe PlainLiteral("")
    results.length mustBe 2
  }

  it should "handle graph parameter" in {
    val queries = ArrayBuffer[String]()
    val queryCollector = new QueryExecutor {
      override protected val resultLang: Lang = ResultSetLang.RS_XML
      def execute(query: String): InputStream = {
        queries.append(query)
        sparqlResults(1).execute(query) // just a dummy
      }
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
    val queryCollector = new QueryExecutor {
      override protected val resultLang: Lang = ResultSetLang.RS_XML
      def execute(query: String): InputStream = {
        queries.append(query)
        sparqlResults(1).execute(query) // just a dummy
      }
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
    val queryCollector = new QueryExecutor {
      override protected val resultLang: Lang = ResultSetLang.RS_XML
      def execute(query: String): InputStream = {
        queries.append(query)
        sparqlResults(1).execute(query) // just a dummy
      }
    }
    val sparqlParams = SparqlParams(pageSize = 1000000)
    val result = PagingSparqlTraversable("SELECT * WHERE { ?s ?p ?o }", queryCollector, sparqlParams, lowLimit)
    result.bindings.head // execute
    queries must have size 1
    queries.head.contains(lowLimit) mustBe true
  }

  it should "stop paging when the thread is interrupted" in {
    val sparqlParams = SparqlParams(pageSize = 1)
    val infiniteLoop = sparqlResults(1)
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
      Thread.sleep(200)
    }
    System.currentTimeMillis() - start must be < 5 * 1000L // If it is not interrupted this should run for minutes
  }

  it should "stop paging when the thread is interrupted while read results from the connection" in {
    val sparqlParams = SparqlParams(pageSize = 1)
    val count = new AtomicInteger(0)
    @volatile var inInputStream = false
    @volatile var interruptedInStream = false
    val blockingExecutor = new QueryExecutor {
      override protected val resultLang: Lang = ResultSetLang.RS_XML
      override def execute(query: String): InputStream = {
        if(count.get() <= 2) {
          sparqlResults(1).execute(query)
        } else {
          val blockingInputStream = new InputStream {
            override def read(): Int = {
              inInputStream = true
              try {
                Thread.sleep(20000)
              } catch {
                case ex: InterruptedException =>
                  interruptedInStream = true
                  throw ex
              }
              fail("Reading should have been interrupted")
            }
          }
          blockingInputStream
        }
      }
    }
    val thread = new Thread {
      override def run(): Unit = {
        val result = PagingSparqlTraversable("SELECT * WHERE { ?s ?p ?o }", blockingExecutor, sparqlParams, limit = Int.MaxValue)
        for(_ <- result.bindings) {
          count.incrementAndGet()
        }
      }
    }
    thread.start()
    val start = System.currentTimeMillis()
    while(thread.isAlive) {
      if(inInputStream) {
        thread.interrupt()
      }
      Thread.sleep(200)
    }
    System.currentTimeMillis() - start must be < 5000L // If it is not interrupted this should run for minutes
    interruptedInStream mustBe true
  }

  it should "inject FROM clauses when a graph is defined and no 'GRAPH selection' is already part of the query" in {
    def mustRewrite(query: String, invert: Boolean = false): Unit = {
      var actualQuery = ""
      val expectedQuery = QueryFactory.create(query).serialize(Syntax.syntaxSPARQL_11)
      def queryExecutor = new QueryExecutor {
        def execute(query: String): InputStream = {
          actualQuery = query
          // Return value doesn't matter, will be ignored
          new ByteArrayInputStream(
            """<sparql xmlns="http://www.w3.org/2005/sparql-results#">
              |</sparql>
              |""".stripMargin.getBytes(Charset.forName("UTF-8")))
        }
      }
      Try(PagingSparqlTraversable(query, queryExecutor, SparqlParams(graph = Some("urn:graph"), pageSize = Int.MaxValue), Int.MaxValue).bindings.head)
      if(invert) {
        actualQuery mustBe expectedQuery
      } else {
        actualQuery must not be expectedQuery
      }
    }
    def mustNotRewrite(query: String): Unit = mustRewrite(query, invert = true)
    mustRewrite("SELECT * WHERE {?s ?p ?o}")
    mustRewrite("SELECT * WHERE {?graph ?p ?o}")
    mustNotRewrite("SELECT * WHERE { GRAPH <urn:someGraph> {?s ?p ?o }}")
    mustNotRewrite("SELECT * WHERE { GRAPH\n<urn:someGraph>\n {?s ?p ?o }}")
    mustNotRewrite("SELECT * WHERE { graph <urn:someGraph> {?s ?p ?o }}")
    mustNotRewrite("SELECT * WHERE { graph\n<urn:someGraph>\n {?s ?p ?o }}")
    mustNotRewrite("SELECT * FROM <urn:someGraph> WHERE { ?s ?p ?o }")
    mustNotRewrite("SELECT * WHERE { GRAPH ?g {?s ?p ?o }}")
  }
}
