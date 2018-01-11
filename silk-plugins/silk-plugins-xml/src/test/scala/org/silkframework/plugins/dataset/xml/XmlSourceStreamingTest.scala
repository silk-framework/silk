package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DataSource
import org.silkframework.runtime.resource.{ClasspathResourceLoader, InMemoryResourceManager}

class XmlSourceStreamingTest extends XmlSourceTestBase {

  override def xmlSource(uriPattern: String): DataSource with XmlSourceTrait = {
    val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/xml/")
    val source = new XmlSourceStreaming(resources.get("persons.xml"), "", uriPattern)
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
    paths.map(_.serialize) shouldBe Seq("/AdditionalAttribute")
  }
}
