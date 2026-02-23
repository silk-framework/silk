package org.silkframework.dataset
import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * An entity sink that forwards all calls to given a sequence of sinks.
  */
class CombinedEntitySink(val sinks: Seq[EntitySink]) extends EntitySink {

  override def openTable(typeUri: Uri, properties: Seq[TypedProperty], singleEntity: Boolean = false)
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    for(sink <- sinks) {
      sink.openTable(typeUri, properties, singleEntity)
    }
  }

  override def closeTable()(implicit userContext: UserContext): Unit = {
    for(sink <- sinks) {
      sink.closeTable()
    }
  }

  override def writeEntity(subject: String, values: IndexedSeq[Seq[String]])
                          (implicit userContext: UserContext): Unit = {
    for(sink <- sinks) {
      sink.writeEntity(subject, values)
    }
  }

  override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {
    for(sink <- sinks) {
      sink.clear(force)
    }
  }

  override def close()(implicit userContext: UserContext): Unit = {
    for(sink <- sinks) {
      sink.close()
    }
  }
}
