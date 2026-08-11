package org.silkframework.cache

import org.silkframework.config.{Blocking, RuntimeConfig}
import org.silkframework.entity.Index

class MemoryEntityCacheTest extends EntityCacheTest {
  behavior of "MemoryEntityCache"

  override protected def withCache(partitionSize: Int)(test: EntityCache => Unit): Unit = {
    test(new MemoryEntityCache(schema, _ => Index.default, runtimeConfig(partitionSize)))
  }

  private def runtimeConfig(partitionSize: Int): RuntimeConfig =
    RuntimeConfig(blocking = Blocking(isEnabled = false), partitionSize = partitionSize)
}
