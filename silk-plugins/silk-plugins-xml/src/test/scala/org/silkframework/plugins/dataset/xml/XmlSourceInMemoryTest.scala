package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DataSource
import org.silkframework.runtime.resource.ClasspathResourceLoader

class XmlSourceInMemoryTest extends XmlSourceTestBase {

  override def xmlSource(uriPattern: String): DataSource with XmlSourceTrait = {
    val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/xml/")
    val source = new XmlSourceInMemory(resources.get("persons.xml"), "", uriPattern)
    source
  }

}
