package org.silkframework.dataset

import org.silkframework.entity.ValueType

/**
  * An experimental trait to extend sinks to be able to write triples.
  */
trait TripleSink extends AutoCloseable {
  def init(): Unit
  def writeTriple(subject: String, predicate: String, obj: String, valueType: ValueType): Unit
}

trait TripleSinkDataset extends Dataset {
  def tripleSink: TripleSink
}