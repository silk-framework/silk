package de.fuberlin.wiwiss.silk.plugins.dataset.rdf

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.dataset.DataSink
import de.fuberlin.wiwiss.silk.dataset.rdf.SparqlEndpoint
import de.fuberlin.wiwiss.silk.entity.Link

/**
 * A sink for writing to SPARQL/Update endpoints.
 */
class SparqlSink(params: SparqlParams, endpoint: SparqlEndpoint) extends DataSink {

  private val log = Logger.getLogger(classOf[SparqlSink].getName)

  /**Maximum number of statements per request. */
  private val StatementsPerRequest = 200

  private val body: StringBuilder = new StringBuilder

  private var statements = 0

  private var properties = Seq[String]()

  override def open(properties: Seq[String]) {
    this.properties = properties
  }

  override def writeLink(link: Link, predicateUri: String) {
    if(body.isEmpty) {
      beginSparul(true)
    } else if (statements + 1 > StatementsPerRequest) {
      endSparql()
      beginSparul(false)
    }

    body.append("<" + link.source + "> <" + predicateUri + "> <" + link.target + "> .\n")
    statements += 1
  }

  override def writeEntity(subject: String, values: Seq[Set[String]]) {
    if(body.isEmpty) {
      beginSparul(true)
    } else if (statements + 1 > StatementsPerRequest) {
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
    // Check if value is an URI
    if (value.startsWith("http:"))
      body.append("<" + subject + "> <" + property + "> <" + value + "> .\n")
    // Check if value is a number
    else if (value.nonEmpty && value.forall(c => c.isDigit || c == '.' || c == 'E'))
      body.append("<" + subject + "> <" + property + "> \"" + value + "\"^^<http://www.w3.org/2001/XMLSchema#double> .\n")
    // Write string values
    else {
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
    if (params.graph.isEmpty) {
      body.append("INSERT DATA { ")
    }
    else {
      //if (newGraph) {
      //  body.append("CREATE SILENT GRAPH {" + params.graph + "}")
      //}
      body.append("INSERT DATA { GRAPH <" + params.graph + "> { ")
    }
    statements = 0
  }

  /**
   * Ends the current SPARQL/Update request.
   */
  private def endSparql() {
    if(params.graph.isEmpty)
      body.append("}")
    else
      body.append("} }")
    val query = body.toString()
    body.clear()
    endpoint.update(query)
  }
}
