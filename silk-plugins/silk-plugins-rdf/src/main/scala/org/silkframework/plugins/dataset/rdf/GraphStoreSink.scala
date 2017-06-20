package org.silkframework.plugins.dataset.rdf

import java.io.BufferedOutputStream
import java.util.logging.Logger

import org.silkframework.dataset.rdf.GraphStoreTrait
import org.silkframework.dataset.{EntitySink, LinkSink, TripleSink, TypedProperty}
import org.silkframework.entity.{Link, ValueType}
import org.silkframework.plugins.dataset.rdf.formatters.RdfFormatter

/**
  * An RDF sink based on the graph store protocol.
  *
  * @see https://www.w3.org/TR/sparql11-http-rdf-update/
  */
case class GraphStoreSink(graphStore: GraphStoreTrait,
                          graphUri: String,
                          formatterOpt: Option[RdfFormatter]) extends EntitySink with LinkSink with TripleSink with RdfSink {
  private var properties = Seq[TypedProperty]()
  private var output: Option[BufferedOutputStream] = None
  private val log = Logger.getLogger(classOf[SparqlSink].getName)
  private var stmtCount = 0

  override def open(properties: Seq[TypedProperty]): Unit = {
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
    stmtCount = 0
    if (output.isDefined) {
      log.warning("Calling init() on already initialized graph store output stream.")
    }
    output = initOutputStream
  }

  private def initOutputStream: Option[BufferedOutputStream] = {
    // Always use N-Triples because of stream-ability
    val out = graphStore.postDataToGraph(graphUri)
    val bufferedOut = new BufferedOutputStream(out)
    Some(bufferedOut)
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
        o.write(stmtString.getBytes("UTF-8"))
      case None =>
        throw new IllegalStateException("Writing to a closed Graph Store output stream!")
    }
  }

  override def writeTriple(subject: String, predicate: String, obj: String, valueType: ValueType): Unit = {
    writeStatement(subject, predicate, obj, valueType)
  }

  override def clear(): Unit = {
    graphStore.deleteGraph(graphUri)
  }

  override def close(): Unit = {
    output match {
      case Some(o) =>
        o.flush()
        o.close()
        output = None
      case None =>
        throw new IllegalStateException("Called close() on non-open graph store output stream!")
    }
  }
}
