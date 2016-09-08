package org.silkframework.plugins.dataset.rdf

import java.io.ByteArrayOutputStream
import java.net.URI

import com.hp.hpl.jena.datatypes.RDFDatatype
import com.hp.hpl.jena.graph.impl.LiteralLabelFactory
import com.hp.hpl.jena.graph.{Node, NodeFactory, Triple}
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.vocabulary.XSD
import org.apache.jena.riot.RDFDataMgr
import org.silkframework.util.StringUtils
import org.silkframework.util.StringUtils.DoubleLiteral

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Created on 8/31/16.
  */
object RdfFormatUtil {
  private val model = ModelFactory.createDefaultModel()
  def tripleValuesToNTriplesSyntax(subject: String, property: String, value: String): String = {
    value match {
      // Check if value is an URI
      case v: String if value.startsWith("http") && Try(URI.create(value)).isSuccess =>
        serializeTriple(subject, property, NodeFactory.createURI(v))
      // Check if value is a number
      case StringUtils.integerNumber() =>
        serializeTriple(subject, property, model.createTypedLiteral(value, XSD.integer.getURI).asNode())
      case DoubleLiteral(d) =>
        serializeTriple(subject, property, model.createTypedLiteral(value, XSD.xdouble.getURI).asNode())
      // Write string values
      case _ =>
        serializeTriple(subject, property, NodeFactory.createLiteral(value))
    }
  }

  /**
    * Serialize a single triple.
    *
    * @param subject Subject URI
    * @param property Property URI
    * @param node A Jena [[Node]].
    * @return
    */
  def serializeTriple(subject: String, property: String, node: Node): String = {
    val output = new ByteArrayOutputStream()
    val triple = new Triple(NodeFactory.createURI(subject), NodeFactory.createURI(property), node)
    RDFDataMgr.writeTriples(output, Iterator(triple).asJava)
    output.toString()
  }
}
