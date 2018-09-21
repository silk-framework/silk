package org.silkframework.workspace.activity

/**
  * A cached activity that reads and writes the serialized XML via streaming to reduce the memory footprint
  */
trait CachedActivityStreaming[T] extends CachedActivity[T] {
  def writeValueStreamed()
}
