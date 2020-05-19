package org.silkframework.dataset
import org.silkframework.entity.{Link, StringValueType, ValueType}
import org.silkframework.runtime.activity.UserContext
import TableLinkSink._
import org.silkframework.config.Prefixes

/**
  * Generic link sink that write links to a table with two columns.
  * The columns are named ''source'' and ''target''.
  * At the moment, all links are written to the same type ''links''.
  */
class TableLinkSink(entitySink: EntitySink) extends LinkSink {

  override def init()(implicit userContext: UserContext): Unit = {
    implicit val prefixes = Prefixes.empty
    entitySink.openTable(LINKS_TYPE, Seq(
      TypedProperty(SOURCE_COLUMN, ValueType.STRING, isBackwardProperty = false),
      TypedProperty(TARGET_COLUMN, ValueType.STRING, isBackwardProperty = false)))
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

object TableLinkSink {

  final val LINKS_TYPE: String = "links"

  final val SOURCE_COLUMN: String = "source"

  final val TARGET_COLUMN: String = "target"

}
