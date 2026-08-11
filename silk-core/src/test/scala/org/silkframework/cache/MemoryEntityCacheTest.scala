package org.silkframework.cache

import org.silkframework.entity.Index

class MemoryEntityCacheTest extends EntityCacheTest {
  behavior of "MemoryEntityCache"

  override protected def withCache(partitionSize: Int)(test: EntityCache => Unit): Unit = {
    test(new MemoryEntityCache(schema, _ => Index.default, runtimeConfig(partitionSize)))
  }
}
