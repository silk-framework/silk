package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DataSource
import org.silkframework.runtime.resource.{ClasspathResourceLoader, InMemoryResourceManager}

class XmlSourceStreamingTest extends XmlSourceTestBase {

  override def xmlSource(name: String, uriPattern: String, baseType: String = ""): DataSource with XmlSourceTrait = {
    val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/xml/")
    val source = new XmlSourceStreaming(resources.get(name), baseType, uriPattern)
    source
  }

  private val xml =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<!DOCTYPE Attributes [
      |<!ELEMENT AdditionalAttribute EMPTY>
      |]>
      |<Attributes>
      |  <AdditionalAttribute />
      |</Attributes>""".stripMargin

  it should "process XML files with DTDs" in {
    val xmlResource = InMemoryResourceManager().get("file.xml")
    xmlResource.writeString(xml)
    val xmlDataset = XmlDataset(xmlResource)
    val paths = xmlDataset.source.retrievePaths("")
    paths.map(_.toUntypedPath.normalizedSerialization) shouldBe Seq("AdditionalAttribute")
  }

  it should "collect the correct values" in {
    val source = xmlSource("persons.xml", "", "Person")
    source.retrievePaths("Properties")
      .map(p => p.serialize() -> p.valueType.id) shouldBe Seq(
      "Property" -> "UriValueType",
      "Property/Key" -> "StringValueType",
      "Property/Value" -> "StringValueType",
      "Property/Key" -> "UriValueType",
      "Property/Key/@id" -> "StringValueType"
    )
  }
}
