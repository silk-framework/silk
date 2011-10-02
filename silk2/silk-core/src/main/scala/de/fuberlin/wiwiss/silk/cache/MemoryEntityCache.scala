package de.fuberlin.wiwiss.silk.cache

import collection.mutable.ArrayBuffer
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import de.fuberlin.wiwiss.silk.entity.{Index, Entity, EntityDescription}

/**
 * An entity cache, which caches the entities in memory and allows adding new entities at runtime.
 */
class MemoryEntityCache(val entityDesc: EntityDescription,
                        val indexFunction: (Entity => Index),
                        runtimeConfig: RuntimeConfig = RuntimeConfig()) extends EntityCache {

  private val logger = Logger.getLogger(getClass.getName)

  private var blocks = IndexedSeq.tabulate(blockCount)(new Block(_))

  private var allEntities = Set[String]()

  private var entityCounter = 0

  @volatile private var writing = false

  /**
   * Writes to this cache.
   */
  override def write(entities: Traversable[Entity]) {
    val startTime = System.currentTimeMillis()
    writing = true

    try {
      for (entity <- entities) {
        add(entity)
      }

      val time = ((System.currentTimeMillis - startTime) / 1000.0)
      logger.info("Finished writing " + entityCounter + " entities with type '" + entityDesc.restrictions + "' in " + time + " seconds")
    }
    finally {
      writing = false
    }
  }

  override def isWriting = writing

  /**
   * Adds a single entity to the cache.
   */
  private def add(entity: Entity) {
    if (!allEntities.contains(entity.uri)) {
      val indices = if(runtimeConfig.blocking.isEnabled) indexFunction(entity).flatten else Set(0)

      for ((block, index) <- indices.groupBy(i => math.abs(i % blockCount))) {
        blocks(block).add(entity, BitsetIndex.build(index))
      }

      allEntities += entity.uri
      entityCounter += 1
    }
  }

  override def clear() {
    entityCounter = 0
    blocks = IndexedSeq.tabulate(blockCount)(new Block(_))
    allEntities = Set[String]()
  }

  override def close() { }

  def entityCount = entityCounter

  /**
   * Reads a partition of a block.
   */
  override def read(block: Int, partition: Int) = blocks(block)(partition)

  override def blockCount: Int = runtimeConfig.blocking.enabledBlocks

  /**
   * The number of partitions in a specific block.
   */
  override def partitionCount(block: Int) = blocks(block).size

  private class Block(block: Int) {
    private val entities = ArrayBuffer(ArrayBuffer[Entity]())
    private val indices = ArrayBuffer(ArrayBuffer[BitsetIndex]())

    def apply(index: Int) = Partition(entities(index).toArray, indices(index).toArray)

    def add(entity: Entity, index: BitsetIndex) {
      if (entities.last.size < runtimeConfig.partitionSize) {
        entities.last.append(entity)
        indices.last.append(index)
      }
      else {
        entities.append(ArrayBuffer(entity))
        indices.append(ArrayBuffer(index))
        logger.info("Written partition " + (entities.size - 2) + " of block " + block)
      }
    }

    def size = entities.size
  }

}
