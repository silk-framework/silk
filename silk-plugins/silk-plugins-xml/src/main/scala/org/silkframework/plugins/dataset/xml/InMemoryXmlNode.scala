package org.silkframework.plugins.dataset.xml

import java.util

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
//    def atResult = {
//      lazy val y = asSeq.head
//      val attr =
//        if (selector.length == 1) fail
//        else if (selector(1) == '{') {
//          val i = selector indexOf '}'
//          if (i == -1) fail
//          val (uri, key) = (selector.substring(2,i), selector.substring(i+1, selector.length()))
//          if (uri == "" || key == "") fail
//          else y.attribute(uri, key)
//        }
//        else y.attribute(selector drop 1)
//
//      attr match {
//        case Some(x)  => Group(x)
//        case _        => NodeSeq.Empty
//      }
//    }

    selector match {
      case ""                                         => fail
      case "_"                                        => filterArray(child, !_.isInstanceOf[InMemoryXmlText])
      case _ if (selector(0) == '@' && asArray.length == 1)  => this match {
        case elem: InMemoryXmlElem => selectAttribute(selector, elem)
        case _ => new Array[InMemoryXmlNode](0)
      }
      case _                                          => filterArray(child, _.label == selector)
    }
  }

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

  private def filterArray(arr: Array[InMemoryXmlNode], cond: (InMemoryXmlNode) => Boolean): Array[InMemoryXmlNode] = {
    var idx = 0
    val indices = new util.ArrayList[Int]()
    while(idx < arr.length) {
      if(cond(arr(idx))) {
        indices.add(idx)
      }
      idx += 1
    }
    val outputArr = new Array[InMemoryXmlNode](indices.size())
    idx = 0
    while(idx < indices.size()) {
      outputArr(idx) = arr(indices.get(idx))
      idx += 1
    }
    outputArr
  }
}

case class InMemoryXmlElem(label: String, override val attributes: Map[String, String], override val child: Array[InMemoryXmlNode]) extends InMemoryXmlNode {
  override def appendText(sb: StringBuffer): Unit = {
    var idx = 0
    while(idx < child.length) {
      child(idx).appendText(sb)
      idx += 1
    }
  }
}

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

case class InMemoryXmlText(value: String) extends InMemoryXmlNode {
  override def appendText(sb: StringBuffer): Unit = sb.append(value)

  override def label: String = "#PCDATA"
}

case class InMemoryXmlAttribute(value: String)

object InMemoryXmlNode {
  def fromNode(node: Node): InMemoryXmlNode = {
    node match {
      case Text(text) => InMemoryXmlText(text)
      case Unparsed(text) => InMemoryXmlText(text)
      case PCData(text) => InMemoryXmlText(text)
      case Group(nodes) => InMemoryXmlNodes(nodes.map(fromNode).toArray)
      case e: Elem => InMemoryXmlElem(e.label, e.attributes.asAttrMap, e.child.map(fromNode).toArray)
      case _ => throw new IllegalArgumentException(node.getClass.getName)
    }
  }

  def attributes(attr: MetaData): Map[String, String] = {
    attr.asAttrMap
  }
}