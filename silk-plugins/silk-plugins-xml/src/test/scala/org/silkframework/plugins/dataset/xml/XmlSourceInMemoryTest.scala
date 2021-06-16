package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DataSource
import org.silkframework.runtime.resource.ClasspathResourceLoader

class XmlSourceInMemoryTest extends XmlSourceTestBase {

  override def xmlSource(name: String, uriPattern: String, baseType: String = ""): DataSource with XmlSourceTrait = {
    val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/xml/")
    val source = new XmlSourceInMemory(resources.get(name), baseType, uriPattern)
    source
  }

  it should "process complex paths with backward operators" in {
    val persons = XmlDoc("persons.xml")
    val events = persons atPath("Person/Events")
    events.valuesAt("""\..[Name = "Max Doe"]/ID""") shouldBe Seq(Seq("1"))
    events.valuesAt("""\..[Name = "Max No"]/ID""") shouldBe Seq(Seq())
  }
}
