package org.silkframework.plugins.dataset.xml

import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

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
    implicit val traverser: XmlTraverser = XmlTraverser(xml)
    evaluate("/A/B") mustBe Seq("Value", "complex element")
    val objectResources = evaluate("/A/B", ValueType.URI)
    objectResources.size mustBe 6
    objectResources.map(_.take(14)).distinct mustBe Seq("urn:instance:B")
    evaluate("/A", ValueType.URI) mustBe Seq("urn:instance:A#1133478415")
    evaluate("/A", ValueType.STRING) mustBe Seq()
    evaluate("/A", ValueType.INT) mustBe Seq()
  }

  it should "not fail for tags that contain dots" in {
    val xml = <A.B>Value</A.B>
    val traverser: XmlTraverser = XmlTraverser(xml)
    // Should not fail (CMEM-3752)
    traverser.generateUri("") mustBe "urn:instance:AB#310208258"
  }

  private def evaluate(pathStr: String, valueType: ValueType = ValueType.STRING)(implicit traverser: XmlTraverser) = {
    traverser.evaluatePathAsString(typedPath(pathStr, valueType), "")
  }

  def typedPath(path: String, valueType: ValueType): TypedPath = TypedPath(UntypedPath.parse(path), valueType, isAttribute = false)
}
