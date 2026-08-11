package org.silkframework.cache

import org.silkframework.config.{Blocking, RuntimeConfig}
import org.silkframework.entity.Index
import org.silkframework.util.FileUtils._

import java.nio.file.Files

class FileEntityCacheTest extends EntityCacheTest {
  behavior of "FileEntityCache"

  override protected def withCache(partitionSize: Int)(test: EntityCache => Unit): Unit = {
    val dir = Files.createTempDirectory("entityCacheTest").toFile
    try {
      test(new FileEntityCache(schema, _ => Index.default, dir, runtimeConfig(partitionSize)))
    } finally {
      dir.deleteRecursive()
    }
  }

  private def runtimeConfig(partitionSize: Int): RuntimeConfig =
    RuntimeConfig(blocking = Blocking(isEnabled = false), partitionSize = partitionSize)
}
