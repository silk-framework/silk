package org.silkframework.rule

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Task}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.similarity.Comparison
import org.silkframework.runtime.plugin.types.IdentifierOptionParameter
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext, XmlSerialization}
import org.silkframework.util.{DPair, Identifier}

class LinkSpecXmlSerializationTest extends AnyFlatSpec with Matchers {

  behavior of "LinkSpec.XmlFormat"

  private implicit val readContext: ReadContext = TestReadContext()

  it should "read back a serialized link specification" in {
    // The identifier is added by the task serialization, so a link specification on its own does not have one
    val xml = XmlSerialization.toXml(linkSpec)
    XmlSerialization.fromXml[LinkSpec](xml) mustBe linkSpec
  }

  it should "read back a serialized linking task" in {
    val task = PlainTask("linkingTask", linkSpec)
    val xml = XmlSerialization.toXml[Task[LinkSpec]](task)
    XmlSerialization.fromXml[Task[LinkSpec]](xml).data mustBe linkSpec
  }

  private def linkSpec: LinkSpec = {
    LinkSpec(
      source = DatasetSelection(IdentifierOptionParameter(Some(Identifier("sourceDs")))),
      target = DatasetSelection(IdentifierOptionParameter(Some(Identifier("targetDs")))),
      rule = LinkageRule(Some(
        Comparison(
          id = Identifier("compareLabels"),
          metric = EqualityMetric(),
          inputs = DPair(
            PathInput(id = Identifier("label1"), path = UntypedPath("label")),
            PathInput(id = Identifier("label2"), path = UntypedPath("label"))
          )
        )
      ))
    )
  }
}
