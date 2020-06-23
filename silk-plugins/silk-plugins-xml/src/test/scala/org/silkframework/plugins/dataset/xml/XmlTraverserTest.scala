package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}

class XmlTraverserTest extends FlatSpec with MustMatchers {
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
    implicit val traverser: XmlTraverser = XmlTraverser(xml)
    evaluate("/A/B") mustBe Seq("Value", "complex element")
    val objectResources = evaluate("/A/B", ValueType.URI)
    objectResources.size mustBe 6
    objectResources.map(_.take(14)).distinct mustBe Seq("urn:instance:B")
    evaluate("/A", ValueType.URI) mustBe Seq("urn:instance:A#1031906387")
    evaluate("/A", ValueType.STRING) mustBe Seq()
    evaluate("/A", ValueType.INT) mustBe Seq()
  }

  private def evaluate(pathStr: String, valueType: ValueType = ValueType.STRING)(implicit traverser: XmlTraverser) = {
    traverser.evaluatePathAsString(typedPath(pathStr, valueType), "")
  }

  def typedPath(path: String, valueType: ValueType): TypedPath = TypedPath(UntypedPath.parse(path), valueType, isAttribute = false)
}
