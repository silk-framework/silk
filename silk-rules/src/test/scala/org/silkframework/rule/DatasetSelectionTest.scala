package org.silkframework.rule

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.Prefixes
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext, XmlSerialization}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{Identifier, Uri}

import scala.xml.{Elem, Null, UnprefixedAttribute}

class DatasetSelectionTest extends AnyFlatSpec with Matchers {

  behavior of "DatasetSelection"

  implicit private val readContext: ReadContext = TestReadContext()

  it should "round-trip a blank selection in XML" in {
    val xml = DatasetSelection.empty.toXML(asSource = true, Prefixes.empty)
    (xml \ "@dataSource").text shouldBe ""
    DatasetSelection.fromXML(xml).inputTaskId shouldBe None
  }

  it should "round-trip a configured selection in XML" in {
    val selection = DatasetSelection("myDataset", Uri("http://example.org/type"))
    val roundTrip = DatasetSelection.fromXML(selection.toXML(asSource = false, Prefixes.empty))
    roundTrip.inputTaskId shouldBe Some(Identifier("myDataset"))
    roundTrip.typeUri.uri shouldBe "http://example.org/type"
  }

  it should "throw a clear validation error if a required input is not configured" in {
    val error = intercept[ValidationException] {
      DatasetSelection.empty.requiredInputId()
    }
    error.getMessage should include ("No input source has been configured")
  }

  it should "round-trip a link spec with blank inputs in XML" in {
    // The id attribute is added by the enclosing task serialization and required by the XSD
    val xml = XmlSerialization.toXml(LinkSpec()).asInstanceOf[Elem] % new UnprefixedAttribute("id", "linkTask", Null)
    val roundTrip = XmlSerialization.fromXml[LinkSpec](xml)
    roundTrip.source.inputTaskId shouldBe None
    roundTrip.target.inputTaskId shouldBe None
  }

  it should "round-trip a transform spec with blank input in XML" in {
    val roundTrip = XmlSerialization.fromXml[TransformSpec](XmlSerialization.toXml(TransformSpec()))
    roundTrip.selection.inputTaskId shouldBe None
  }
}
