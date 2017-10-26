package org.silkframework.plugins.dataset.csv

import org.silkframework.dataset.{LinkSink, TypedProperty}
import org.silkframework.entity.{Link, StringValueType}
import org.silkframework.runtime.resource.WritableResource

/**
 * Created by andreas on 12/11/15.
 */
class CsvLinkSink(file: WritableResource, settings: CsvSettings) extends CsvSink(file, settings) with LinkSink {

  /**
    * Initialize the link sink
    */
  override def init(): Unit = {
    openTable("",
      Seq(TypedProperty("link source", StringValueType, isBackwardProperty = false),
        TypedProperty("link target", StringValueType, isBackwardProperty = false)))
  }

  override def writeLink(link: Link, predicateUri: String) {
    write(Seq(link.source, link.target))
  }
}
