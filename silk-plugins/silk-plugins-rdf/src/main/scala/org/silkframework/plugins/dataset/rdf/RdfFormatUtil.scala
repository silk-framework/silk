package org.silkframework.plugins.dataset.rdf

import org.apache.jena.graph.{Node, NodeFactory}
import org.apache.jena.rdf.model.{AnonId, ModelFactory, Statement}
import org.apache.jena.riot.adapters.RDFWriterFactoryRIOT
import org.apache.jena.vocabulary.XSD
import org.apache.jena.sparql.core.{Quad => JenaQuad}
import org.apache.jena.graph.{Triple => JenaTriple}
import org.silkframework.dataset.rdf._
import org.silkframework.entity._
import org.silkframework.rule.util.JenaSerializationUtil
import org.silkframework.util.{StringUtils, Uri}

/** Utility methods for serializing to RDF */
object RdfFormatUtil {
  final val BOOLEAN_JENA_TYPE = NodeFactory.getType(XSD.xboolean.getURI)
  final val DOUBLE_JENA_TYPE = NodeFactory.getType(XSD.xdouble.getURI)
  final val FLOAT_JENA_TYPE = NodeFactory.getType(XSD.xfloat.getURI)
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
    val graph = q.context.map(c => NodeFactory.createURI(c.value)).orNull

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
    val tripleString = JenaSerializationUtil.serializeTriple(subject, property, objNode)
    valueType match {
      case CustomValueType(typeUri) if ValueType.URI.validate(typeUri) =>
        val cutLine = tripleString.dropRight(3) // Hack, since Jena does not provide any way to use arbitrary types without implementing a custom type
        cutLine + s"^^<$typeUri> .\n"
      case _ =>
        tripleString
    }
  }

  def resolveObjectValue(lexicalValue: String, valueType: ValueType): Node = {
    valueType match {
      case CustomValueType(typeUri) if ValueType.URI.validate(typeUri) =>
        model.createLiteral(lexicalValue).asNode() // Hack: Jena needs an implementation for each type URI, so serialize as String and attach datatype later
      case ValueType.URI if ValueType.URI.validate(lexicalValue) =>
        model.createResource(lexicalValue).asNode
      case ValueType.STRING | ValueType.UNTYPED =>
        model.createLiteral(lexicalValue).asNode
      case LanguageValueType(lang) =>
        model.createLiteral(lexicalValue, lang).asNode
      case ValueType.BLANK_NODE =>
        model.createResource(new AnonId(lexicalValue)).asNode
      case dateType: DateAndTimeValueType =>
        model.createTypedLiteral(lexicalValue, dateType.xmlSchemaType(lexicalValue)).asNode()
      case ValueType.DATE_TIME =>
        model.createTypedLiteral(lexicalValue, ValueType.DATE_TIME.xmlSchemaType(lexicalValue)).asNode()
      case valueType: ValueType if valueType.uri.isDefined =>
        model.createTypedLiteral(lexicalValue, valueType.uri.get).asNode()
      case _ =>
        throw new IllegalArgumentException(s"Cannot create RDF node from value type $valueType and lexical string '$lexicalValue'! Validation failed.")
    }
  }
}
