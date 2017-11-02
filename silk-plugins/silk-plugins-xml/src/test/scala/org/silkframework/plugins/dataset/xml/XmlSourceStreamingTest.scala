package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DataSource
import org.silkframework.runtime.resource.ClasspathResourceLoader

class XmlSourceStreamingTest extends XmlSourceTestBase {

  override def xmlSource(uriPattern: String): DataSource = {
    val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/xml/")
    val source = new XmlSourceStreaming(resources.get("persons.xml"), "", uriPattern)
    source
  }

}
