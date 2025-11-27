package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DirtyTrackingFileDataSink
import org.silkframework.plugins.dataset.hierarchical.{HierarchicalEntityWriter, HierarchicalSink}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource

import java.io.OutputStream

class XmlSink(val resource: WritableResource,
              outputTemplate: XmlOutputTemplate,
              override val maxDepth: Int = HierarchicalSink.DEFAULT_MAX_SIZE) extends HierarchicalSink with DirtyTrackingFileDataSink {

  protected override val writeAttributesFirst: Boolean = true

  override protected def outputEntities(writeOutput: HierarchicalEntityWriter => Unit): Unit = {
    resource.write() { outputStream =>
      val writer = new XmlEntityWriter(outputStream, outputTemplate)
      try {
        writeOutput(writer)
      } finally {
        writer.close()
      }
    }
  }

  /**
   * Makes sure that the next write will start from an empty dataset.
   */
  override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {
    resource.delete()
  }
}