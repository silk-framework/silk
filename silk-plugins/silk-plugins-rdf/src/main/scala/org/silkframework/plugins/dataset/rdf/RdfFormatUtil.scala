package org.silkframework.plugins.dataset.rdf

import java.io.ByteArrayOutputStream
import java.net.URI

import com.hp.hpl.jena.graph.{Node, NodeFactory, Triple}
import com.hp.hpl.jena.rdf.model.{AnonId, ModelFactory}
import com.hp.hpl.jena.vocabulary.XSD
import org.apache.jena.riot.RDFDataMgr
import org.silkframework.entity._
import org.silkframework.util.StringUtils
import org.silkframework.util.StringUtils.DoubleLiteral

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Created on 8/31/16.
  */
object RdfFormatUtil {
  private val model = ModelFactory.createDefaultModel()
  def tripleValuesToNTriplesSyntax(subject: String, property: String, value: String, valueType: ValueType): String = {
    val objNode = resolveObjectValue(value, valueType)
    val tripleString = serializeTriple(subject, property, objNode)
    valueType match {
      case CustomValueType(typeUri) if UriValueType.validate(typeUri) =>
        val cutLine = tripleString.dropRight(3) // Hack, since Jena does not provide any way to use arbitrary types without implementing a custom type
        cutLine + s"^^<$typeUri> .\n"
      case _ =>
        tripleString
    }
  }

  private def autoDetectValueType(value: String): Node = {
    value match {
      // Check if value is an URI
      case v: String if value.startsWith("http") && Try(URI.create(value)).isSuccess =>
        NodeFactory.createURI(v)
      // Check if value is a number
      case StringUtils.integerNumber() =>
        model.createTypedLiteral(value, XSD.integer.getURI).asNode
      case DoubleLiteral(d) =>
        model.createTypedLiteral(value, XSD.xdouble.getURI).asNode
      // Write string values
      case _ =>
        NodeFactory.createLiteral(value)
    }
  }

  def resolveObjectValue(lexicalValue: String, valueType: ValueType): Node = {
    valueType match {
      case AutoDetectValueType =>
        autoDetectValueType(lexicalValue)
      case CustomValueType(typeUri) =>
        model.createLiteral(lexicalValue).asNode()
      case UriValueType =>
        model.createResource(lexicalValue).asNode
      case StringValueType =>
        model.createLiteral(lexicalValue).asNode
      case LanguageValueType(lang) =>
        model.createLiteral(lexicalValue, lang).asNode
      case BlankNodeValueType =>
        model.createResource(new AnonId(lexicalValue)).asNode
      case _ =>
        // TODO: Support all ValueTypes with the appropriate method
        autoDetectValueType(lexicalValue)
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