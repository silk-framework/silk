package org.silkframework.plugins.dataset.rdf

import java.io.ByteArrayOutputStream
import java.util.logging.Logger

import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlParams}
import org.silkframework.dataset.{EntitySink, LinkSink, TripleSink, TypedProperty}
import org.silkframework.entity.{Link, ValueType}
import org.silkframework.plugins.dataset.rdf.formatters.RdfFormatter

/**
 * A sink for writing to SPARQL/Update endpoints.
 */
class SparqlSink(params: SparqlParams,
                 endpoint: SparqlEndpoint,
                 formatterOpt: Option[RdfFormatter] = None,
                 /**Maximum number of statements per request. */
                 statementsPerRequest: Int = 200) extends EntitySink with LinkSink with TripleSink {

  private val log = Logger.getLogger(classOf[SparqlSink].getName)

  private val body: StringBuilder = new StringBuilder

  private var statements = 0

  private var properties = Seq[TypedProperty]()

  override def open(properties: Seq[TypedProperty]) {
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
    *
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
    for((property, valueSet) <- properties zip values; value <- valueSet) {
      if(property.isBackwardProperty) {
        writeStatement(value, property.propertyUri, subject, property.valueType)
      } else {
        writeStatement(subject, property.propertyUri, value, property.valueType)
      }
    }
  }

  override def close() {
    if(body.nonEmpty) {
      endSparql()
    }
  }

  override def clear(): Unit = {
    params.graph match {
      case Some(graph) =>
        endpoint.update(s"DROP SILENT GRAPH <$graph>")
      case None =>
        endpoint.update(s"DROP SILENT DEFAULT")
    }
  }

  def writeStatement(subject: String, property: String, value: String, valueType: ValueType): Unit = {
    if(body.isEmpty) {
      beginSparul(true)
    } else if (statements + 1 > statementsPerRequest) {
      endSparql()
      beginSparul(false)
    }

    val stmtString: String = buildStatementString(subject, property, value, valueType)
    body.append(stmtString)
    statements += 1
  }

  def buildStatementString(subject: String, property: String, value: String, valueType: ValueType): String = {
    RdfFormatUtil.tripleValuesToNTriplesSyntax(subject, property, value, valueType)
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

  override def writeTriple(subject: String, predicate: String, obj: String, valueType: ValueType): Unit = {
    writeStatement(subject, predicate, obj, valueType)
  }
}
