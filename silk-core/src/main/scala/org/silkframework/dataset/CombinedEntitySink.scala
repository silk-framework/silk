package org.silkframework.dataset
import org.silkframework.util.Uri

/**
  * An entity sink that forwards all calls to given a sequence of sinks.
  */
class CombinedEntitySink(sinks: Seq[EntitySink]) extends EntitySink {

  override def openTable(typeUri: Uri, properties: Seq[TypedProperty]): Unit = {
    for(sink <- sinks) {
      sink.openTable(typeUri, properties)
    }
  }

  override def closeTable(): Unit = {
    for(sink <- sinks) {
      sink.closeTable()
    }
  }

  override def writeEntity(subject: String, values: Seq[Seq[String]]): Unit = {
    for(sink <- sinks) {
      sink.writeEntity(subject, values)
    }
  }

  override def clear(): Unit = {
    for(sink <- sinks) {
      sink.clear()
    }
  }

  override def close(): Unit = {
    for(sink <- sinks) {
      sink.close()
    }
  }
}
