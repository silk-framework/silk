package org.silkframework.plugins.dataset.rdf

import java.io.ByteArrayOutputStream
import java.net.URI

import org.apache.jena.graph.{Node, NodeFactory, Triple}
import org.apache.jena.rdf.model.{AnonId, ModelFactory}
import org.apache.jena.vocabulary.XSD
import org.apache.jena.riot.RDFDataMgr
import org.silkframework.entity._
import org.silkframework.util.{StringUtils, Uri}
import org.silkframework.util.StringUtils.DoubleLiteral

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Created on 8/31/16.
  */
object RdfFormatUtil {
  final val BOOLEAN_JENA_TYPE = NodeFactory.getType(XSD.xboolean.getURI)
  final val DOUBLE_JENA_TYPE = NodeFactory.getType(XSD.xdouble.getURI)
  final val FLOAT_JENA_TYPE = NodeFactory.getType(XSD.xfloat.getURI)
  final val INT_JENA_TYPE = NodeFactory.getType(XSD.xint.getURI)
  final val INTEGER_JENA_TYPE = NodeFactory.getType(XSD.integer.getURI)
  final val LONG_JENA_TYPE = NodeFactory.getType(XSD.xlong.getURI)

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
      case v: String if (value.startsWith("http") || value.startsWith("urn")) && new Uri(v).isValidUri =>
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
      case CustomValueType(typeUri) if UriValueType.validate(typeUri) =>
        model.createLiteral(lexicalValue).asNode() // Hack: Jena needs an implementation for each type URI, so serialize as String and attach datatype later
      case UriValueType if UriValueType.validate(lexicalValue) =>
        model.createResource(lexicalValue).asNode
      case StringValueType =>
        model.createLiteral(lexicalValue).asNode
      case LanguageValueType(lang) =>
        model.createLiteral(lexicalValue, lang).asNode
      case BlankNodeValueType =>
        model.createResource(new AnonId(lexicalValue)).asNode
      case BooleanValueType =>
        model.createTypedLiteral(lexicalValue, BOOLEAN_JENA_TYPE).asNode()
      case DoubleValueType =>
        model.createTypedLiteral(lexicalValue, DOUBLE_JENA_TYPE).asNode()
      case FloatValueType =>
        model.createTypedLiteral(lexicalValue, FLOAT_JENA_TYPE).asNode()
      case IntValueType =>
        model.createTypedLiteral(lexicalValue, INT_JENA_TYPE).asNode()
      case IntegerValueType =>
        model.createTypedLiteral(lexicalValue, INTEGER_JENA_TYPE).asNode()
      case LongValueType =>
        model.createTypedLiteral(lexicalValue, LONG_JENA_TYPE).asNode()
      case DateTimeValueType =>
        model.createTypedLiteral(lexicalValue, DateTimeValueType.xmlSchemaType(lexicalValue)).asNode()
      case _ =>
        throw new IllegalArgumentException(s"Cannot create RDF node from value type $valueType and lexical string $lexicalValue! Validation failed.")
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