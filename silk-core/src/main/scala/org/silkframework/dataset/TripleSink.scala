package org.silkframework.dataset

/**
  * An experimental trait to extend sinks to be able to write triples.
  */
trait TripleSink extends AutoCloseable {
  def init(): Unit
  def writeTriple(subject: String, predicate: String, obj: String): Unit
}

trait TripleSinkDataset extends Dataset {
  def tripleSink: TripleSink
}