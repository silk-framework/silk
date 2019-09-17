package org.silkframework.dataset
import org.silkframework.entity.{Link, StringValueType}
import org.silkframework.runtime.activity.UserContext

/**
  * Generic link sink that write links to a table with two columns.
  * The columns are named ''source'' and ''target''.
  * At the moment, all links are written to the same type ''links''.
  */
class TableLinkSink(entitySink: EntitySink) extends LinkSink {

  override def init()(implicit userContext: UserContext): Unit = {
    entitySink.openTable("links", Seq(
      TypedProperty("source", StringValueType, isBackwardProperty = false),
      TypedProperty("target", StringValueType, isBackwardProperty = false)))
  }

  override def writeLink(link: Link, predicateUri: String)
                        (implicit userContext: UserContext){
    entitySink.writeEntity(link.source, Seq(Seq(link.source), Seq(link.target)))
  }

  override def close()(implicit userContext: UserContext): Unit = {
    entitySink.closeTable()
    entitySink.close()
  }

  override def clear()(implicit userContext: UserContext): Unit = {
    entitySink.clear()
  }
}
