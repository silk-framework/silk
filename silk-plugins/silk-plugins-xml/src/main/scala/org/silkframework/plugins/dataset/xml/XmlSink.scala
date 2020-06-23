package org.silkframework.plugins.dataset.xml

import java.io.StringReader

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl
import org.silkframework.config.Prefixes
import org.silkframework.dataset.{EntitySink, TypedProperty}
import org.silkframework.entity.{UriValueType, ValueType}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri
import org.w3c.dom.{Document, Element, Node, ProcessingInstruction}

import scala.collection.mutable
import scala.xml.InputSource

class XmlSink(resource: WritableResource, outputTemplate: String) extends EntitySink {

  private var doc: Document = null

  private var entityTemplate: ProcessingInstruction = null

  private var entityRoot: Node = null

  private var atRoot: Boolean = true

  private var properties: Seq[TypedProperty] = Seq.empty

  private var uriMap: mutable.HashMap[String, mutable.HashSet[Element]] = mutable.HashMap()


  /**
    * Initializes this writer.
    *
    * @param properties The list of properties of the entities to be written.
    */
  override def openTable(typeUri: Uri, properties: Seq[TypedProperty])
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    if(atRoot) {
      val builder = DocumentBuilderFactory.newInstance.newDocumentBuilder
      // Check if the output template is a single processing instruction
      if(outputTemplate.matches("<\\?[^\\?]+\\?>")) {
        val elementName = outputTemplate.substring(2, outputTemplate.length - 2)
        doc = builder.newDocument()
        entityTemplate = doc.createProcessingInstruction(elementName, "")
        entityRoot = doc
      } else {
        doc = builder.parse(new InputSource(new StringReader(outputTemplate)))
        entityTemplate = findEntityTemplate(doc)
        entityRoot = entityTemplate.getParentNode
        entityRoot.removeChild(entityTemplate)
      }
    }

    this.properties = properties
  }

  /**
    * Writes a new entity.
    *
    * @param subjectURI The subject URI of the entity.
    * @param values  The list of values of the entity. For each property that has been provided
    *                when opening this writer, it must contain a set of values.
    */
  override def writeEntity(subjectURI: String, values: Seq[Seq[String]])
                          (implicit userContext: UserContext): Unit = {
    val entityNodes = getEntityNodes(subjectURI)
    for {
      (property, valueSeq) <- properties zip values if property.propertyUri != "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
      value <- valueSeq
      entityNode <- entityNodes
    } {
      addValue(entityNode, property, value)
    }
  }

  override def closeTable()(implicit userContext: UserContext): Unit = {
    atRoot = false
  }

  override def close()(implicit userContext: UserContext): Unit = {
    val transformerFactory = new TransformerFactoryImpl() // We have to specify this here explicitly, else it will take the Saxon implementation
    val transformer = transformerFactory.newTransformer

    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    // This is implementation specific, but there is no standard way of setting the indent amount
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    resource.write()(os => transformer.transform(new DOMSource(doc), new StreamResult(os)))
  }

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  override def clear()(implicit userContext: UserContext): Unit = {
    doc = null
    entityTemplate = null
    entityRoot = null
    atRoot = true
    properties = Seq.empty
    uriMap = mutable.HashMap()
  }

  private def findEntityTemplate(node: Node): ProcessingInstruction = {
    findEntityTemplateRecursive(node) match {
      case Some(pi) =>
        pi
      case None =>
        throw new ValidationException("Could not find template entity of the form <?Entity?>")
    }
  }

  private def findEntityTemplateRecursive(node: Node): Option[ProcessingInstruction] = {
    if(node.isInstanceOf[ProcessingInstruction]) {
      Some(node.asInstanceOf[ProcessingInstruction])
    } else if(node.hasChildNodes) {
      val children = node.getChildNodes
      for(i <- 0 until children.getLength) {
        findEntityTemplateRecursive(children.item(i)) match {
          case pi @ Some(_) =>
            return pi
          case None => // Do nothing
        }
      }
      None
    } else {
      None
    }
  }

  /**
    * Gets the XML nodes for the given entity URI
    */
  private def getEntityNodes(entityURI: String): Set[Element] = {
    if(atRoot) {
      val entityNode = doc.createElement(entityTemplate.getTarget)
      if(entityRoot.getParentNode == null && entityRoot.getFirstChild != null) {
        throw new ValidationException("Cannot insert more than one element at document root. Your output template definition " +
            "only allows one entity. Either adapt sink input to be one entity or adapt output template.")
      }
      entityRoot.appendChild(entityNode)
      Set(entityNode)
    } else {
      uriMap.get(entityURI) match {
        case Some(parentNode) =>
          parentNode.toSet
        case None =>
          throw new ValidationException("Could not find parent for " + entityURI)
      }
    }
  }

  /**
    * Adds a single property value to a XML node.
    */
  private def addValue(entityNode: Element, property: TypedProperty, value: String): Unit = {
    property.valueType match {
      case ValueType.URI =>
        val elements = uriMap.getOrElseUpdate(value, mutable.HashSet.empty[Element])
        if(property.propertyUri.isEmpty) { // Empty target on object mapping, stay on same target node
          elements += entityNode
        } else {
          val valueNode = newElement(property.propertyUri)
          elements += valueNode.asInstanceOf[Element]
          entityNode.appendChild(valueNode)
        }
      case _ if property.isAttribute =>
        setAttribute(entityNode, property.propertyUri, value)
      case _ if property.propertyUri == "#text" =>
        entityNode.setTextContent(value)
      case _ =>
        val valueNode = newElement(property.propertyUri)
        valueNode.setTextContent(value)
        entityNode.appendChild(valueNode)
    }
  }

  /**
    * Generates an empty XML element from a URI.
    */
  private def newElement(uri: String): Element = {
    val separatorIndex = uri.lastIndexWhere(c => c == '/' || c == '#')
    if(separatorIndex == -1) {
      doc.createElement(uri)
    } else {
      doc.createElementNS(uri.substring(0, separatorIndex + 1), uri.substring(separatorIndex + 1))
    }
  }

  /**
    * Sets an attribute on a node using a URI.
    */
  private def setAttribute(node: Element, uri: String, value: String): Unit = {
    val separatorIndex = uri.lastIndexWhere(c => c == '/' || c == '#')
    if(separatorIndex == -1) {
      node.setAttribute(uri, value)
    } else {
      node.setAttributeNS(uri.substring(0, separatorIndex + 1), uri.substring(separatorIndex + 1), value)
    }
  }
}
