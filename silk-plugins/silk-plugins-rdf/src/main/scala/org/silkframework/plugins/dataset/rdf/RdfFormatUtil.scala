package org.silkframework.plugins.dataset.rdf

import java.io.ByteArrayOutputStream

import org.apache.jena.graph.{Node, NodeFactory}
import org.apache.jena.rdf.model.{AnonId, ModelFactory, Statement}
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.adapters.RDFWriterFactoryRIOT
import org.apache.jena.vocabulary.XSD
import org.apache.jena.sparql.core.{Quad => JenaQuad}
import org.apache.jena.graph.{Triple => JenaTriple}
import org.silkframework.dataset.rdf._
import org.silkframework.entity._
import org.silkframework.util.{StringUtils, Uri}

import scala.collection.JavaConverters._

/** Utility methods for serializing to RDF */
object RdfFormatUtil {
  final val BOOLEAN_JENA_TYPE = NodeFactory.getType(XSD.xboolean.getURI)
  final val DOUBLE_JENA_TYPE = NodeFactory.getType(XSD.xdouble.getURI)
  final val FLOAT_JENA_TYPE = NodeFactory.getType(XSD.xfloat.getURI)
  final val INT_JENA_TYPE = NodeFactory.getType(XSD.xint.getURI)
  final val INTEGER_JENA_TYPE = NodeFactory.getType(XSD.integer.getURI)
  final val LONG_JENA_TYPE = NodeFactory.getType(XSD.xlong.getURI)

  val WriterFactory = new RDFWriterFactoryRIOT()


  /**
    * Converts a [[Quad]] into a [[JenaQuad]]
    * @param q - the origin Quad
    */
  def quadToJenaQuad(q: Quad): JenaQuad ={
    val subj = q.subject match{
      case r: Resource => NodeFactory.createURI(r.value)
      case b: BlankNode => NodeFactory.createBlankNode(b.value)
    }
    val pred = NodeFactory.createURI(q.predicate.value)
    val obj = q.objectVal match{
      case r: Resource => NodeFactory.createURI(r.value)
      case b: BlankNode => NodeFactory.createBlankNode(b.value)
      case ll: LanguageLiteral => NodeFactory.createLiteral(ll.value, ll.language)
      case pl: PlainLiteral => NodeFactory.createLiteral(pl.value)
      case tl: DataTypeLiteral => NodeFactory.createLiteral(tl.value, NodeFactory.getType(tl.dataType))
    }
    val graph = NodeFactory.createURI(q.context.value)

    new JenaQuad(graph, subj, pred, obj)
  }

  private def getObject(n: Node): RdfNode ={
    if(n.isBlank){
      BlankNode(n.getBlankNodeLabel)
    }
    else if(n.isLiteral) {
      if(n.getLiteralLanguage != null && n.getLiteralLanguage.nonEmpty){
        LanguageLiteral(n.getLiteral.getLexicalForm, n.getLiteralLanguage)
      }
      else if(n.getLiteralDatatype != null){
        DataTypeLiteral(n.getLiteral.getLexicalForm, n.getLiteralDatatypeURI)
      }
      else{
        PlainLiteral(n.getLiteral.getLexicalForm)
      }
    }
    else{
      Resource(n.getURI)
    }
  }

  /**
    * Converts a Jena Quad to a (Silk) Quad object
    * NOTE: when using this function in connection with the [[QuadIterator]] constructor, make sure to forward the QueryExecution close function
    * @param q - the Jena Quad object
    */
  def jenaQuadToQuad(q: JenaQuad): Quad = {
    val subj = q.getSubject
    if(subj.isBlank){
      Quad(BlankNode(subj.getBlankNodeLabel), Resource(q.getPredicate.getURI), getObject(q.getObject), Resource(q.getGraph.getURI))
    }
    else{
      Quad(Resource(subj.getURI), Resource(q.getPredicate.getURI), getObject(q.getObject), Resource(q.getGraph.getURI))
    }
  }

  /**
    * Converts a Jena Triple to a (Silk) Triple object
    * NOTE: when using this function in connection with the [[TripleIterator]] constructor, make sure to forward the QueryExecution close function
    * @param q - the Jena Triple object
    */
  def jenaTripleToTriple(q: JenaTriple): Triple = {
    val subj = q.getSubject
    if(subj.isBlank){
      Triple(BlankNode(subj.getBlankNodeLabel), Resource(q.getPredicate.getURI), getObject(q.getObject))
    }
    else{
      Triple(Resource(subj.getURI), Resource(q.getPredicate.getURI), getObject(q.getObject))
    }
  }

  /**
    * Converts a Jena Statement to a Quad object
    * NOTE: when using this function in connection with the [[QuadIterator]] constructor, make sure to forward the StatementIterator close function
    * @param q - the Jena Statement
    */
  def jenaStatementToTriple(q: Statement): Triple = {
    val subj = q.getSubject.asNode()
    if(subj.isBlank){
      Triple(BlankNode(subj.getBlankNodeLabel), Resource(q.getPredicate.getURI), getObject(q.getObject.asNode()))
    }
    else{
      Triple(Resource(subj.getURI), Resource(q.getPredicate.getURI), getObject(q.getObject.asNode()))
    }
  }

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
      case StringUtils.simpleDoubleNumber() =>
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
      case DateValueType =>
        model.createTypedLiteral(lexicalValue, DateValueType.xmlSchemaType(lexicalValue)).asNode()
      case DateTimeValueType =>
        model.createTypedLiteral(lexicalValue, DateTimeValueType.xmlSchemaType(lexicalValue)).asNode()
      case _ =>
        throw new IllegalArgumentException(s"Cannot create RDF node from value type $valueType and lexical string '$lexicalValue'! Validation failed.")
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
    val triple = new JenaTriple(NodeFactory.createURI(subject), NodeFactory.createURI(property), node)
    RDFDataMgr.writeTriples(output, Iterator(triple).asJava)
    output.toString()
  }

  def serializeSingleNode(node: Node): String = {
    val subjectPropertyLength = 8
    val spaceDotNewLineLength = 3
    val output = new ByteArrayOutputStream()
    val triple = new JenaTriple(NodeFactory.createURI("a"), NodeFactory.createURI("b"), node)
    RDFDataMgr.writeTriples(output, Iterator(triple).asJava)
    output.toString().drop(subjectPropertyLength).dropRight(spaceDotNewLineLength)
  }
}
