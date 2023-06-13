package org.silkframework.dataset
import org.silkframework.config.Prefixes
import org.silkframework.dataset.TableLinkSink._
import org.silkframework.entity.{Link, ValueType}
import org.silkframework.runtime.activity.UserContext

/**
  * Generic link sink that write links to a table with two columns.
  * The columns are named ''source'' and ''target''.
  * At the moment, all links are written to the same type ''links''.
  */
class TableLinkSink(entitySink: EntitySink) extends LinkSink {

  override def init()(implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    entitySink.openTable(LINKS_TYPE, Seq(
      TypedProperty(SOURCE_COLUMN, ValueType.STRING, isBackwardProperty = false, isAttribute = true),
      TypedProperty(TARGET_COLUMN, ValueType.STRING, isBackwardProperty = false, isAttribute = true)), singleEntity = false)
  }

  override def writeLink(link: Link, predicateUri: String, inversePredicateUri: Option[String])
                        (implicit userContext: UserContext, prefixes: Prefixes){
    entitySink.writeEntity(link.source, IndexedSeq(Seq(link.source), Seq(link.target)))
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
