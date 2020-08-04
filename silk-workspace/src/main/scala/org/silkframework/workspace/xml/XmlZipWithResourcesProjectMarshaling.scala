package org.silkframework.workspace.xml

import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "xmlZipMarshalling",
  label = "XML/ZIP file",
  description = "ZIP archive, which includes XML meta data and resource files."
)
case class XmlZipWithResourcesProjectMarshaling() extends XmlZipProjectMarshaling {

  def id: String = XmlZipWithResourcesProjectMarshaling.marshallerId

  val name = "XML/ZIP file (with resources)"

  def includeResources: Boolean = true
}

object XmlZipWithResourcesProjectMarshaling {

  val marshallerId = "xmlZip"

}
