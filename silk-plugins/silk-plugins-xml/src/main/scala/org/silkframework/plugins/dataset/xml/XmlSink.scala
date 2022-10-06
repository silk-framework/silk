package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DirtyTrackingFileDataSink
import org.silkframework.plugins.dataset.hierarchical.{HierarchicalEntityWriter, HierarchicalSink}
import org.silkframework.runtime.resource.WritableResource

import java.io.OutputStream

class XmlSink(val resource: WritableResource,
              outputTemplate: XmlOutputTemplate,
              override val maxDepth: Int = HierarchicalSink.DEFAULT_MAX_SIZE) extends HierarchicalSink with DirtyTrackingFileDataSink {

  protected override val writeAttributesFirst: Boolean = true

  override protected def createWriter(outputStream: OutputStream): HierarchicalEntityWriter = {
    new XmlEntityWriter(outputStream, outputTemplate)
  }
}