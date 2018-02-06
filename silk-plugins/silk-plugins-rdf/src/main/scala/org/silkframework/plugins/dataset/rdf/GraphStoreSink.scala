package org.silkframework.plugins.dataset.rdf

import java.io.{BufferedOutputStream, OutputStream}
import java.util.logging.Logger

import org.silkframework.dataset.rdf.GraphStoreTrait
import org.silkframework.dataset.{EntitySink, LinkSink, TripleSink, TypedProperty}
import org.silkframework.entity.{Link, ValueType}
import org.silkframework.plugins.dataset.rdf.formatters.RdfFormatter
import org.silkframework.util.Uri

/**
  * An RDF sink based on the graph store protocol.
  *
  * @see https://www.w3.org/TR/sparql11-http-rdf-update/
  */
case class GraphStoreSink(graphStore: GraphStoreTrait,
                          graphUri: String,
                          formatterOpt: Option[RdfFormatter],
                          comment: Option[String],
                          dropGraphOnClear: Boolean) extends EntitySink with LinkSink with TripleSink with RdfSink {

  private var properties = Seq[TypedProperty]()
  private var output: Option[OutputStream] = None
  private val log = Logger.getLogger(classOf[SparqlSink].getName)
  private var stmtCount = 0
  private var byteCount = 0L
  private val maxBytesPerRequest = graphStore.defaultTimeouts.maxRequestSize // in bytes

  override def openTable(typeUri: Uri, properties: Seq[TypedProperty]): Unit = {
    init()
    this.properties = properties
  }

  override def writeEntity(subject: String, values: Seq[Seq[String]]): Unit = {
    for((property, valueSet) <- properties zip values; value <- valueSet) {
      if(property.isBackwardProperty) {
        writeStatement(value, property.propertyUri, subject, property.valueType)
      } else {
        writeStatement(subject, property.propertyUri, value, property.valueType)
      }
    }
  }

  override def writeLink(link: Link, predicateUri: String): Unit = {
    val (newStatements, _) = formatLink(link, predicateUri)
    writeStatementString(newStatements)
  }

  override def init(): Unit = {
    if(output.isEmpty) {
      stmtCount = 0
      byteCount = 0L
      output = initOutputStream
    }
  }

  private def initOutputStream: Option[OutputStream] = {
    // Always use N-Triples because of stream-ability
    val out = graphStore.postDataToGraph(graphUri, comment = comment)
    Some(out)
  }

  override def writeStatement(subject: String, property: String, value: String, valueType: ValueType): Unit = {
    val stmtString: String = buildStatementString(subject, property, value, valueType)
    writeStatementString(stmtString)
    stmtCount += 1
  }

  // Writes an N-Triples statement to the output stream.
  private def writeStatementString(stmtString: String) = {
    output match {
      case Some(o) =>
        val outBytes = stmtString.getBytes("UTF-8")
        val outputLength = outBytes.length
        if(byteCount + outputLength > maxBytesPerRequest) {
          close()
          init()
        }
        byteCount += outputLength
        o.write(outBytes)
      case None =>
        throw new IllegalStateException("Writing to a closed Graph Store output stream!")
    }
  }

  override def writeTriple(subject: String, predicate: String, obj: String, valueType: ValueType): Unit = {
    writeStatement(subject, predicate, obj, valueType)
  }

  override def clear(): Unit = {
    if(dropGraphOnClear) {
      graphStore.deleteGraph(graphUri)
    }
  }

  override def closeTable(): Unit = {}

  override def close(): Unit = {
    output match {
      case Some(o) =>
        try {
          o.close()
        } finally {
          output = None
        }
      case None =>
        // no effect
    }
  }
}
