package org.silkframework.plugins.dataset.rdf

import java.io.ByteArrayOutputStream
import java.util.logging.Logger

import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.silkframework.dataset.rdf.{SparqlParams, SparqlEndpoint}
import org.silkframework.dataset.{EntitySink, LinkSink}
import org.silkframework.entity.Link
import org.silkframework.plugins.dataset.rdf.formatters.RdfFormatter
import org.silkframework.util.StringUtils.DoubleLiteral

/**
 * A sink for writing to SPARQL/Update endpoints.
 */
class SparqlSink(params: SparqlParams,
                 endpoint: SparqlEndpoint,
                 formatterOpt: Option[RdfFormatter] = None,
                 /**Maximum number of statements per request. */
                 statementsPerRequest: Int = 200) extends EntitySink with LinkSink {

  private val log = Logger.getLogger(classOf[SparqlSink].getName)

  private val body: StringBuilder = new StringBuilder

  private var statements = 0

  private var properties = Seq[String]()

  override def open(properties: Seq[String]) {
    this.properties = properties
  }

  override def init() = {}

  override def writeLink(link: Link, predicateUri: String) {
    val (newStatements, statementCount) = formatLink(link, predicateUri)
    if(body.isEmpty) {
      beginSparul(true)
    } else if (statements + statementCount > statementsPerRequest) {
      endSparql()
      beginSparul(false)
    }

    body.append(newStatements)
    statements += statementCount
  }

  /**
   * Returns the RDF formatted link in N-Triples format and the number of triples.
   * @param link
   * @param predicateUri
   * @return (serialized statements as N-Triples, triple count)
   */
  private def formatLink(link: Link,
                         predicateUri: String): (String, Int) = {
    formatterOpt match {
      case Some(formatter) =>
        val model = formatter.formatAsRDF(link, predicateUri)
        val outputStream = new ByteArrayOutputStream()
        RDFDataMgr.write(outputStream, model, Lang.NTRIPLES)
        outputStream.flush()
        outputStream.close()
        val result = outputStream.toString("UTF-8")
        (result, result.split("\n").length)
      case None =>
        val result = "<" + link.source + "> <" + predicateUri + "> <" + link.target + "> .\n"
        (result, 1)
    }
  }

  override def writeEntity(subject: String, values: Seq[Seq[String]]) {
    if(body.isEmpty) {
      beginSparul(true)
    } else if (statements + 1 > statementsPerRequest) {
      endSparql()
      beginSparul(false)
    }

    for((property, valueSet) <- properties zip values; value <- valueSet) {
      writeStatement(subject, property, value)
    }
  }

  override def close() {
    if(body.nonEmpty) {
      endSparql()
    }
  }

  private def writeStatement(subject: String, property: String, value: String): Unit = {
    value match {
      // Check if value is an URI
      case v if value.startsWith("http:") || value.startsWith("https:") =>
        body.append("<" + subject + "> <" + property + "> <" + v + "> .\n")
      // Check if value is a number
      case DoubleLiteral(d) =>
        body.append("<" + subject + "> <" + property + "> \"" + d + "\"^^<http://www.w3.org/2001/XMLSchema#double> .\n")
      // Write string values
      case _ =>
        val escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        body.append("<" + subject + "> <" + property + "> \"" + escapedValue + "\" .\n")
    }

    statements += 1
  }

  /**
   * Begins a new SPARQL/Update request.
   *
   * @param newGraph Create a new (empty) graph?
   */
  private def beginSparul(newGraph: Boolean) {
    body.clear()
    params.graph match {
      case None =>
        body.append("INSERT DATA { ")
      case Some(graph) =>
        //if (newGraph) {
        //  body.append("CREATE SILENT GRAPH {" + params.graph + "}")
        //}
        body.append("INSERT DATA { GRAPH <" + graph + "> { ")
    }
    statements = 0
  }

  /**
   * Ends the current SPARQL/Update request.
   */
  private def endSparql() {
    params.graph match {
      case None => body.append("}")
      case Some(g) => body.append("} }")
    }
    val query = body.toString()
    body.clear()
    if(statements > 0) { // Else this would throw an exception, because of invalid syntax
      endpoint.update(query)
    }
  }
}
