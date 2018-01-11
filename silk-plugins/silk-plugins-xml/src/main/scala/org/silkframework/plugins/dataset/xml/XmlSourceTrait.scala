package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DataSource
import org.silkframework.entity.Path
import org.silkframework.util.Uri

/**
  * A trait that all XML data source implementations should implement
  */
trait XmlSourceTrait { this: DataSource =>
  def retrieveXmlPaths(typeUri: Uri, depth: Int, limit: Option[Int], onlyLeafNodes: Boolean, onlyInnerNodes: Boolean): IndexedSeq[Path]
}
