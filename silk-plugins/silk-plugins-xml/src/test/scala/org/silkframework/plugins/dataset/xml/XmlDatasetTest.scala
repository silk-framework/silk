package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.plugin.MultilineStringParameter
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.validation.ValidationException
import org.xml.sax.SAXParseException

class XmlDatasetTest extends FlatSpec with MustMatchers {
  behavior of "XML dataset"

  it should "only accept valid output templates" in {
    // Should not throw exception
    testOutputTemplate("<?Root?>")
    testOutputTemplate("<Root><?Entity?></Root>")
    // Should throw exception
    intercept[ValidationException] {
      testOutputTemplate("no XML element")
    }
    intercept[ValidationException] {
      testOutputTemplate("<Root><?Entity?><?Entity2?></Root>")
    }
    intercept[SAXParseException] {
      testOutputTemplate("<Root><?Entity?></Root2>")
    }
  }

  private def testOutputTemplate(outputTemplate: String) = {
    XmlDataset(InMemoryResourceManager.apply().get("test.xml"), outputTemplate = MultilineStringParameter(outputTemplate))
  }
}
