package org.silkframework.plugins.dataset.json

import org.silkframework.dataset.DirtyTrackingFileDataSink
import org.silkframework.plugins.dataset.hierarchical.{HierarchicalEntityWriter, HierarchicalSink}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource

import java.io.OutputStream

class JsonSink (val resource: WritableResource,
                template: JsonTemplate = JsonTemplate.default,
                override val maxDepth: Int = HierarchicalSink.DEFAULT_MAX_SIZE) extends HierarchicalSink with DirtyTrackingFileDataSink {

  override protected def outputEntities(writeOutput: HierarchicalEntityWriter => Unit): Unit = {
    resource.write() { outputStream =>
      val writer = new JsonEntityWriter(outputStream, template)
      try {
        writeOutput(writer)
      } finally {
        writer.close()
      }
    }
  }

  /**
   * Only deletes the file if forced, because each write replaces the file anyway.
   */
  override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {
    if(force) {
      resource.delete()
      super.clear(force)
    }
  }

}
