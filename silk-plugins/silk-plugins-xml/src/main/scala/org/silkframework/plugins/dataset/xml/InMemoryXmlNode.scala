package org.silkframework.plugins.dataset.xml

import scala.xml._
import java.lang.StringBuilder

/**
  * Minimalistic in-memory model of an XML node
  */
sealed trait InMemoryXmlNode {
  /** Append text of node to string buffer */
  def appendText(sb: StringBuilder): Unit

  /**
    * Concatenated text enclosed in "", to be used in property filters.
    */
  lazy val textExpression: String = {
    val sb = new java.lang.StringBuilder()
    sb.append('\"')
    appendText(sb)
    sb.append('\"')
    sb.toString
  }

  /** Returns concatenated text of this XML node. */
  def text: String = {
    val sb = new StringBuilder()
    appendText(sb)
    sb.toString
  }

  /** Label of the XML node, e.g. element tag name */
  def label: String

  /** Attributes of the XML node */
  def attributes: Map[String, String] = Map.empty

  /** Children of the XML node */
  def child: Array[InMemoryXmlNode] = Array.empty

  def asArray: Array[InMemoryXmlNode] = {
    val array = new Array[InMemoryXmlNode](1)
    array(0) = this
    array
  }

  def id: String = this.hashCode.toString.replace('-', '1')
}

/** Represents an XML element */
case class InMemoryXmlElem(override val id: String,
                           label: String,
                           override val attributes: Map[String, String],
                           override val child: Array[InMemoryXmlNode]) extends InMemoryXmlNode {
  override def appendText(sb: StringBuilder): Unit = {
    var idx = 0
    while(idx < child.length) {
      child(idx).appendText(sb)
      idx += 1
    }
  }
}

/** Represents a group of nodes. */
case class InMemoryXmlNodes(nodes: Array[InMemoryXmlNode]) extends InMemoryXmlNode {
  override def appendText(sb: StringBuilder): Unit = {
    var idx = 0
    while(idx < nodes.length) {
      nodes(idx).appendText(sb)
      idx += 1
    }
  }

  private def notSupported(method: String): Nothing = throw new UnsupportedOperationException(s"class Nodes does not support method '$method'")

  override def label: String = notSupported("label")

  override def attributes: Map[String, String] = notSupported("label")

  override def child: Array[InMemoryXmlNode] = notSupported("child")

  override def asArray: Array[InMemoryXmlNode] = nodes
}

/** XML Text node */
case class InMemoryXmlText(value: String) extends InMemoryXmlNode {
  override def appendText(sb: StringBuilder): Unit = sb.append(value)

  override def label: String = "#PCDATA"
}

object InMemoryXmlNode {
  /** Convert Scala XML node to [[InMemoryXmlNode]] */
  def fromNode(node: Node): InMemoryXmlNode = {
    node match {
      case Text(text) => InMemoryXmlText(text)
      case Unparsed(text) => InMemoryXmlText(text)
      case PCData(text) => InMemoryXmlText(text)
      case Group(nodes) => InMemoryXmlNodes(nodes.map(fromNode).toArray)
      case e: Elem => InMemoryXmlElem(e.hashCode.toString.replace('-', '1'), e.label, e.attributes.asAttrMap, e.child.map(fromNode).toArray)
      case _ => throw new IllegalArgumentException(node.getClass.getName)
    }
  }

  /** Convert Scala XML attribute [[MetaData]] object to simple map. */
  def attributes(attr: MetaData): Map[String, String] = {
    attr.asAttrMap
  }
}