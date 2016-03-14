package org.silkframework.plugins.dataset.csv

import org.silkframework.dataset.LinkSink
import org.silkframework.entity.Link
import org.silkframework.runtime.resource.{WritableResource, Resource}

/**
 * Created by andreas on 12/11/15.
 */
class CsvLinkSink(file: WritableResource, settings: CsvSettings) extends CsvSink(file, settings) with LinkSink {

  override def writeLink(link: Link, predicateUri: String) {
    write(link.source + settings.separator + link.target + "\n")
  }

  /**
   * Initialize the link sink
   */
  override def init(): Unit = {
    open(Seq("link source", "link target"))
  }
}
