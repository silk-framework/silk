package org.silkframework.plugins.dataset.xml

import org.silkframework.plugins.dataset.hierarchical.{HierarchicalEntityWriter, HierarchicalSink}
import org.silkframework.runtime.resource.WritableResource

import java.io.OutputStream

class XmlSink(val resource: WritableResource, outputTemplate: String) extends HierarchicalSink {

  override protected def createWriter(outputStream: OutputStream): HierarchicalEntityWriter = {
    new XmlEntityWriter(outputStream, XmlTemplate.parse(outputTemplate))
  }
}