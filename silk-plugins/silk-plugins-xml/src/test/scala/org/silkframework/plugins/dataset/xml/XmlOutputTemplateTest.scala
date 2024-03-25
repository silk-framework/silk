package org.silkframework.plugins.dataset.xml

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class XmlOutputTemplateTest extends AnyFlatSpec with Matchers {

  behavior of "XmlOutputTemplate"

  it should "accept basic output templates" in {
    val template = XmlOutputTemplate.parse("<Root><?Entity?></Root>")
    template.isRootTemplate shouldBe false
    template.rootElementName shouldBe "Entity"
  }

  it should "accept output templates with a single root element" in {
    val rootTemplate = XmlOutputTemplate.parse("<?Root?>")
    rootTemplate.isRootTemplate shouldBe true
    rootTemplate.rootElementName shouldBe "Root"
  }

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

  it should "accept output templates with DTD sections" in {
    val template = XmlOutputTemplate.parse(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "https://eccenca.com/nonExistentDTD.dtd">
        |<Root>
        |  <?Entity?>
        |</Root>""".stripMargin)
    template.isRootTemplate shouldBe false
    template.rootElementName shouldBe "Entity"
  }

}
