package org.silkframework.plugins.dataset.xml

import org.silkframework.util.Identifier

import scala.xml._

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
    val sb = new StringBuilder()
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

  /**
   * Position of this XML node in the source file.
   */
  def position: XmlPosition

  def asArray: Array[InMemoryXmlNode] = {
    val array = new Array[InMemoryXmlNode](1)
    array(0) = this
    array
  }

  def id: String = this.hashCode.toString.replace('-', '1')
}

/** Represents an XML element */
case class InMemoryXmlElem(label: String,
                           override val attributes: Map[String, String],
                           override val child: Array[InMemoryXmlNode],
                           override val position: XmlPosition) extends InMemoryXmlNode {

  override def id: String = s"${position.line}-${position.column}"

  override def appendText(sb: StringBuilder): Unit = {
    var idx = 0
    while(idx < child.length) {
      child(idx).appendText(sb)
      idx += 1
    }
  }
}

/** Represents a group of nodes. */
case class InMemoryXmlNodes(nodes: Array[InMemoryXmlNode],
                            override val position: XmlPosition) extends InMemoryXmlNode {
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
case class InMemoryXmlText(value: String,
                           override val position: XmlPosition) extends InMemoryXmlNode {
  override def appendText(sb: StringBuilder): Unit = sb.append(value)

  override def label: String = "#PCDATA"
}

/** XML attribute node. */
case class InMemoryXmlAttribute(attributeName: String,
                                value: String,
                                override val position: XmlPosition) extends InMemoryXmlNode {
  override def appendText(sb: StringBuilder): Unit = sb.append(value)

  override def label: String = {
    val nodeLabel = Identifier.fromAllowed(attributeName.replace("@", "attr_"))
    if(!nodeLabel.startsWith("attr_")) {
      s"attr_$nodeLabel"
    } else {
      nodeLabel
    }
  }
}

object InMemoryXmlNode {

  /** Convert Scala XML attribute [[MetaData]] object to simple map. */
  def attributes(attr: MetaData): Map[String, String] = {
    attr.asAttrMap
  }
}

/**
 * @param line Line number of the XML node
 * @param column Column position of the XML node
 */
case class XmlPosition(line: Int, column: Int)
