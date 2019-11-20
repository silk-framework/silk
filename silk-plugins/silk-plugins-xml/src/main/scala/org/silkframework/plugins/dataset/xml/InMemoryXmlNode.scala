package org.silkframework.plugins.dataset.xml

import scala.xml._

/**
  * Minimalistic in-memory model of an XML node
  */
sealed trait InMemoryXmlNode {
  /** Append text of node to string buffer */
  def appendText(sb: StringBuffer): Unit

  /** Returns concatenated text of this XML node. */
  def text: String = {
    val sb = new StringBuffer()
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

  /** Projection function, which returns  elements of `this` sequence based
    *  on the string `that`. Use:
    *   - `this \ "foo"` to get a list of all elements that are labelled with `"foo"`;
    *   - `\ "_"` to get a list of all elements (wildcard);
    *   - `ns \ "@foo"` to get the unprefixed attribute `"foo"`;
    *   - `ns \ "@{uri}foo"` to get the prefixed attribute `"pre:foo"` whose
    *     prefix `"pre"` is resolved to the namespace `"uri"`.
    *
    *  For attribute projections, the resulting [[scala.xml.NodeSeq]] attribute
    *  values are wrapped in a [[scala.xml.Group]].
    *
    *  There is no support for searching a prefixed attribute by its literal prefix.
    *
    *  The document order is preserved.
    */
  def childSelect(selector: String): Array[InMemoryXmlNode] = {
    def fail = throw new IllegalArgumentException(selector)

    selector match {
      case "" => fail
      case "_" => ArrayUtil.filterArray(child, !_.isInstanceOf[InMemoryXmlText])
      case _ if selector(0) == '@' && asArray.length == 1 => this match {
        case elem: InMemoryXmlElem => selectAttribute(selector, elem)
        case _ => new Array[InMemoryXmlNode](0)
      }
      case _ => ArrayUtil.filterArray(child, _.label == selector)
    }
  }

  /** fetch value of an attribute, returns empty array if it does not exist */
  private def selectAttribute(selector: String, elem: InMemoryXmlElem): Array[InMemoryXmlNode] = {
    elem.attributes.get(selector.drop(1)) match {
      case Some(attrValue) =>
        val arr = new Array[InMemoryXmlNode](1)
        arr(0) = InMemoryXmlText(attrValue)
        arr
      case None =>
        Array.empty
    }
  }

  def id: String = this.hashCode.toString.replace('-', '1')
}

/** Represents an XML element */
case class InMemoryXmlElem(override val id: String,
                           label: String,
                           override val attributes: Map[String, String],
                           override val child: Array[InMemoryXmlNode]) extends InMemoryXmlNode {
  override def appendText(sb: StringBuffer): Unit = {
    var idx = 0
    while(idx < child.length) {
      child(idx).appendText(sb)
      idx += 1
    }
  }
}

/** Represents a group of nodes. */
case class InMemoryXmlNodes(nodes: Array[InMemoryXmlNode]) extends InMemoryXmlNode {
  override def appendText(sb: StringBuffer): Unit = {
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
  override def appendText(sb: StringBuffer): Unit = sb.append(value)

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