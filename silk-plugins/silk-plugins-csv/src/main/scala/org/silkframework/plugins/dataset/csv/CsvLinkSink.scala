package org.silkframework.plugins.dataset.csv

import org.silkframework.config.Prefixes
import org.silkframework.dataset.{LinkSink, TypedProperty}
import org.silkframework.entity.{Link, ValueType}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource

/**
 * Created by andreas on 12/11/15.
 */
class CsvLinkSink(file: WritableResource, settings: CsvSettings) extends CsvSink(file, settings) with LinkSink {

  /**
    * Initialize the link sink
    */
  override def init()(implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    openTable("",
      Seq(TypedProperty("link_source", ValueType.STRING, isBackwardProperty = false),
        TypedProperty("link_target", ValueType.STRING, isBackwardProperty = false)))
  }

  override def writeLink(link: Link, predicateUri: String, inversePredicateUri: Option[String])
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    write(Seq(link.source, link.target))
  }

  override def close()(implicit userContext: UserContext): Unit = {
    closeTable()
    super.close()
  }
}
