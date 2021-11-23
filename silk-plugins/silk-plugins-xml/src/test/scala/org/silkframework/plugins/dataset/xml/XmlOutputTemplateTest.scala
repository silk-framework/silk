package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, Matchers}

class XmlOutputTemplateTest extends FlatSpec with Matchers {

  behavior of "XmlOutputTemplate"

  it should "fail for templates that are no valid XML" in {
    val exception = the[XmlOutputTemplate.NoValidXmlException] thrownBy {
      XmlOutputTemplate.parse("<invalidXml")
    }
    // Make sure that the actual parsing error is attached
    exception.getCause should not be (null)

    an[XmlOutputTemplate.NoValidXmlException] shouldBe thrownBy {
      XmlOutputTemplate.parse("<? InvalidProcessingInstruction?>")
    }
  }

  it should "fail for templates that do not contain a processing instruction" in {
    an[XmlOutputTemplate.NoProcessingInstructionException] shouldBe thrownBy {
      XmlOutputTemplate.parse("<root></root>")
    }
  }

  it should "fail for templates that do contain multiple processing instructions" in {
    an[XmlOutputTemplate.MultipleProcessingInstructionsException] shouldBe thrownBy {
      XmlOutputTemplate.parse("<Root><?Entity?><?Entity?></Root>")
    }
  }

}
