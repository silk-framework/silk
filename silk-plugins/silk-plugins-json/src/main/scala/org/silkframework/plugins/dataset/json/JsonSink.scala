package org.silkframework.plugins.dataset.json

import org.silkframework.plugins.dataset.hierarchical.{HierarchicalEntityWriter, HierarchicalSink}
import org.silkframework.runtime.resource.WritableResource

import java.io.OutputStream

class JsonSink (val resource: WritableResource, outputSingleJsonObject: Boolean = true, template: JsonTemplate = JsonTemplate.default) extends HierarchicalSink {
  override protected def createWriter(outputStream: OutputStream): HierarchicalEntityWriter = {
    new JsonEntityWriter(outputStream, outputSingleJsonObject, template)
  }

}
