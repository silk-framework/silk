package org.silkframework.plugins.dataset.rdf

import java.util.logging.Logger

import org.silkframework.dataset.DataSink
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.entity.Link

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
    if (value.startsWith("http:") || value.startsWith("https:"))
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
    endpoint.update(query)
  }
}
