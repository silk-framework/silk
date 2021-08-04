package org.silkframework.plugins.dataset.json

import org.silkframework.plugins.dataset.hierarchical.{HierarchicalEntityWriter, HierarchicalSink}
import org.silkframework.runtime.resource.WritableResource

import java.io.OutputStream

class JsonSink (val resource: WritableResource,
                template: JsonTemplate = JsonTemplate.default,
                override val maxDepth: Int = HierarchicalSink.DEFAULT_MAX_SIZE) extends HierarchicalSink {

  override protected def createWriter(outputStream: OutputStream): HierarchicalEntityWriter = {
    new JsonEntityWriter(outputStream, template)
  }

}
