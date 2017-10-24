package org.silkframework.plugins.dataset.xml

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.{OutputKeys, TransformerFactory}

import org.silkframework.dataset.{EntitySink, TypedProperty}
import org.silkframework.entity.UriValueType
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri
import org.w3c.dom.{Document, Element, Node}

class XmlSink(resource: WritableResource, basePath: String, defaultNamespace: String = "urn:schema:") extends EntitySink {

  private val doc: Document =  DocumentBuilderFactory.newInstance.newDocumentBuilder.newDocument

  private var isRoot: Boolean = true

  private var rootNode: Node = doc

  private var childNodeName: String = ""

  private var properties: Seq[TypedProperty] = Seq.empty

  private var uriMap: Map[String, Element] = Map.empty

  /**
    * Initializes this writer.
    *
    * @param properties The list of properties of the entities to be written.
    */
  override def open(typeUri: Uri, properties: Seq[TypedProperty]): Unit = {
    if(isRoot) {
      require(basePath.nonEmpty, "The base path needs to be set to a non empty path, such as \"/Root/Element\"")
      val parts = basePath.stripPrefix("/").split('/')
      for(part <- parts.init) {
        val node = doc.createElementNS(defaultNamespace, part)
        rootNode.appendChild(node)
        rootNode = node
      }
      childNodeName = parts.last
    }

    this.properties = properties
  }

  /**
    * Writes a new entity.
    *
    * @param subject The subject URI of the entity.
    * @param values  The list of values of the entity. For each property that has been provided
    *                when opening this writer, it must contain a set of values.
    */
  override def writeEntity(subject: String, values: Seq[Seq[String]]): Unit = {
    if(isRoot) {
      val entityNode = doc.createElementNS(defaultNamespace, childNodeName)
      rootNode.appendChild(entityNode)
    }

    for {
      (property, valueSeq) <- properties zip values if property.propertyUri != "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
      value <- valueSeq
    } {
      val node = createElement(property.propertyUri)

      property.valueType match {
        case UriValueType =>
          uriMap += ((value, node))
        case _ =>
          node.setTextContent(value)
      }

      if(isRoot) {
        rootNode.getChildNodes.item(rootNode.getChildNodes.getLength - 1).appendChild(node)
      } else {
        uriMap.get(subject) match {
          case Some(parentNode) =>
            parentNode.appendChild(node)
          case None =>
            throw new ValidationException("Could not find parent for " + subject)
        }
      }
    }
  }

  override def close(): Unit = {
    val transformerFactory = TransformerFactory.newInstance
    val transformer = transformerFactory.newTransformer

    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    // This is implementation specific, but there is no standard way of setting the indent amount
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    resource.write()(os => transformer.transform(new DOMSource(doc), new StreamResult(os)))

    isRoot = false
  }

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  override def clear(): Unit = {
  }

  /**
    * Generates an empty XML element from a URI.
    */
  private def createElement(uri: String): Element = {
    if(uri.startsWith(defaultNamespace)) {
      doc.createElementNS(defaultNamespace, uri.stripPrefix(defaultNamespace))
    } else {
      val separatorIndex = uri.lastIndexWhere(c => c == '/' || c == '#')
      if(separatorIndex == -1) {
        doc.createElement(uri)
      } else {
        doc.createElementNS(uri.substring(0, separatorIndex + 1), "ns:" + uri.substring(separatorIndex + 1))
      }
    }
  }
}
