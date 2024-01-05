package org.silkframework.plugins.dataset.xml


import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.xml.{Elem, Group, Node, PCData, Text, Unparsed}

class XmlTraverserTest extends AnyFlatSpec with Matchers {
  behavior of "XML Traverser"

  it should "evaluate paths as requested" in {
    val xml = <Root>
      <A>
        <B>Value</B>
        <B/>
        <B a1="" a2="">complex element</B>
        <B></B>
        <B a1="" a2=""/>
        <B>
          <C>not the same element</C>
        </B>
      </A>
    </Root>
    implicit val traverser: XmlTraverser = createTraverser(xml)
    evaluate("/A/B") mustBe Seq("Value", "complex element")
    val objectResources = evaluate("/A/B", ValueType.URI)
    objectResources.size mustBe 6
    objectResources.map(_.take(14)).distinct mustBe Seq("urn:instance:B")
    evaluate("/A", ValueType.URI) mustBe Seq("urn:instance:A#1-1")
    evaluate("/A", ValueType.STRING) mustBe Seq()
    evaluate("/A", ValueType.INT) mustBe Seq()
  }

  it should "not fail for tags that contain dots" in {
    val xml = <A.B>Value</A.B>
    val traverser: XmlTraverser = createTraverser(xml)
    // Should not fail (CMEM-3752)
    traverser.generateUri("") mustBe "urn:instance:AB#1-1"
  }

  private def evaluate(pathStr: String, valueType: ValueType = ValueType.STRING)(implicit traverser: XmlTraverser) = {
    traverser.evaluatePathAsString(typedPath(pathStr, valueType), "")
  }

  def typedPath(path: String, valueType: ValueType): TypedPath = TypedPath(UntypedPath.parse(path), valueType, isAttribute = false)

  def createTraverser(node: Node): XmlTraverser = {
    XmlTraverser(convertNode(node))
  }

  /** Convert Scala XML node to [[InMemoryXmlNode]] */
  def convertNode(node: Node): InMemoryXmlNode = {
    val pos = XmlPosition(1, 1)
    node match {
      case Text(text) => InMemoryXmlText(text, pos)
      case Unparsed(text) => InMemoryXmlText(text, pos)
      case PCData(text) => InMemoryXmlText(text, pos)
      case Group(nodes) => InMemoryXmlNodes(nodes.map(convertNode).toArray, pos)
      case e: Elem =>
        InMemoryXmlElem(
          label = e.label,
          attributes = e.attributes.map(m => (m.key -> m.value.text)).toMap,
          child = e.child.map(convertNode).toArray,
          position = pos
        )
      case _ => throw new IllegalArgumentException(node.getClass.getName)
    }
  }
}
