package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.{DataSource, TypedPathRetrieveDataSource}
import org.silkframework.runtime.resource.ClasspathResourceLoader

class XmlSourceInMemoryTest extends XmlSourceTestBase {

  override def xmlSource(name: String, uriPattern: String): DataSource with XmlSourceTrait with TypedPathRetrieveDataSource = {
    val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/xml/")
    val source = new XmlSourceInMemory(resources.get(name), "", uriPattern)
    source
  }

}
