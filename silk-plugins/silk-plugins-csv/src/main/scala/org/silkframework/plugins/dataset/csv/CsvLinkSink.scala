package org.silkframework.plugins.dataset.csv

import org.silkframework.dataset.{LinkSink, TypedProperty}
import org.silkframework.entity.{Link, StringValueType}
import org.silkframework.runtime.resource.{Resource, WritableResource}

/**
 * Created by andreas on 12/11/15.
 */
class CsvLinkSink(file: WritableResource, settings: CsvSettings) extends CsvSink(file, settings) with LinkSink {

  /**
    * Initialize the link sink
    */
  override def init(): Unit = {
    open(Seq(TypedProperty("link source", StringValueType), TypedProperty("link target", StringValueType)))
  }

  override def writeLink(link: Link, predicateUri: String) {
    write(Seq(link.source, link.target))
  }
}
