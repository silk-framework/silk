package de.fuberlin.wiwiss.silk.plugins.datasource

import de.fuberlin.wiwiss.silk.datasource.DataSource
import java.io.File
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import de.fuberlin.wiwiss.silk.entity.{FileEntityCache, Entity, EntityDescription}

@Plugin(id = "cache", label = "Cache")
class CacheDataSource(dir: String) extends DataSource {
  private val file = new File(dir)

  def retrieve(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty): Traversable[Entity] = {
    val entityCache = new FileEntityCache(entityDesc, file, RuntimeConfig())

    new Traversable[Entity] {
      override def foreach[U](f: Entity => U) {
        for (block <- 0 until entityCache.blockCount;
             partition <- 0 until entityCache.partitionCount(block);
             entity <- entityCache.read(block, partition).entities) {
          f(entity)
        }
      }
    }
  }
}