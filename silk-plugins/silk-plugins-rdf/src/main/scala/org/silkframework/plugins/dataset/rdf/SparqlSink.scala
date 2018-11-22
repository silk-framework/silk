package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlParams}
import org.silkframework.dataset.{EntitySink, LinkSink, TripleSink, TypedProperty}
import org.silkframework.entity.{Link, ValueType}
import org.silkframework.plugins.dataset.rdf.formatters.RdfFormatter
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
 * A sink for writing to SPARQL/Update endpoints.
 */
class SparqlSink(params: SparqlParams,
                 endpoint: SparqlEndpoint,
                 val formatterOpt: Option[RdfFormatter] = None,
                 /**Maximum number of statements per request. */
                 statementsPerRequest: Int = 200,
                 dropGraphOnClear: Boolean = true) extends EntitySink with LinkSink with TripleSink with RdfSink with SparqlEndpointSink {

  private val body: StringBuilder = new StringBuilder

  private var statements = 0

  private var properties = Seq[TypedProperty]()

  override def sparqlEndpoint: SparqlEndpoint = sparqlEndpoint

  override def openTable(typeUri: Uri, properties: Seq[TypedProperty])
                        (implicit userContext: UserContext): Unit = {
    this.properties = properties
  }

  override def init()(implicit userContext: UserContext): Unit = {}

  override def writeLink(link: Link, predicateUri: String)
                        (implicit userContext: UserContext): Unit = {
    val (newStatements, statementCount) = formatLink(link, predicateUri)
    if(body.isEmpty) {
      beginSparul()
    } else if (statements + statementCount > statementsPerRequest) {
      endSparql()
      beginSparul()
    }

    body.append(newStatements)
    statements += statementCount
  }

  override def writeEntity(subject: String, values: Seq[Seq[String]])
                          (implicit userContext: UserContext): Unit = {
    for((property, valueSet) <- properties zip values; value <- valueSet) {
      if(property.isBackwardProperty) {
        writeStatement(value, property.propertyUri, subject, property.valueType)
      } else {
        writeStatement(subject, property.propertyUri, value, property.valueType)
      }
    }
  }

  override def closeTable()(implicit userContext: UserContext) {}

  override def close()(implicit userContext: UserContext) {
    if(body.nonEmpty) {
      endSparql()
    }
  }

  override def clear()(implicit userContext: UserContext): Unit = {
    if(dropGraphOnClear) {
      params.graph match {
        case Some(graph) =>
          endpoint.update(s"DROP SILENT GRAPH <$graph>")
        case None =>
          endpoint.update(s"DROP SILENT DEFAULT")
      }
    }
  }

  def writeStatement(subject: String, property: String, value: String, valueType: ValueType)
                    (implicit userContext: UserContext): Unit = {
    if(body.isEmpty) {
      beginSparul()
    } else if (statements + 1 > statementsPerRequest) {
      endSparql()
      beginSparul()
    }

    val stmtString: String = buildStatementString(subject, property, value, valueType)
    body.append(stmtString)
    statements += 1
  }

  /**
   * Begins a new SPARQL/Update request.
   */
  private def beginSparul() {
    body.clear()
    params.graph match {
      case None =>
        body.append("INSERT DATA { ")
      case Some(graph) =>
        body.append("INSERT DATA { GRAPH <" + graph + "> { ")
    }
    statements = 0
  }

  /**
   * Ends the current SPARQL/Update request.
   */
  private def endSparql()(implicit userContext: UserContext) {
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

  override def writeTriple(subject: String, predicate: String, obj: String, valueType: ValueType)
                          (implicit userContext: UserContext): Unit = {
    writeStatement(subject, predicate, obj, valueType)
  }
}
