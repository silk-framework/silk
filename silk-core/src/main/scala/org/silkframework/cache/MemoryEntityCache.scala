/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.cache

import org.silkframework.config.RuntimeConfig
import org.silkframework.entity.{Entity, EntitySchema, Index}

import java.util.logging.{Level, Logger}
import scala.collection.mutable.ArrayBuffer

/**
 * An entity cache, which caches the entities in memory and allows adding new entities at runtime.
 */
class MemoryEntityCache(val entitySchema: EntitySchema,
                        val indexFunction: (Entity => Index),
                        runtimeConfig: RuntimeConfig = RuntimeConfig()) extends EntityCache {

  private val logger = Logger.getLogger(getClass.getName)

  @volatile
  private var blocks = IndexedSeq.tabulate(blockCount)(new Block(_))

  @volatile
  private var allEntities = Set[String]()

  @volatile
  private var entityCounter = 0

  /**
   * Adds a single entity to the cache.
   */
  override def write(entity: Entity): Unit = {
    if (!allEntities.contains(entity.uri)) {
      val indices = if(runtimeConfig.blocking.isEnabled) indexFunction(entity).flatten else Set(0)

      for ((block, index) <- indices.groupBy(i => math.abs(i % blockCount))) {
        blocks(block).add(entity, BitsetIndex.build(index))
      }

      allEntities += entity.uri
      entityCounter += 1
    }
  }

  override def clear(): Unit = {
    logger.log(Level.FINE, "Clearing the memory cache.")
    entityCounter = 0
    blocks = IndexedSeq.tabulate(blockCount)(new Block(_))
    allEntities = Set[String]()
  }

  override def close(): Unit = {
    // Publish each block's partially filled last partition.
    for (block <- blocks) {
      block.seal()
    }
  }

  override def size: Int = entityCounter

  /**
   * Reads a partition of a block.
   */
  override def read(block: Int, partition: Int): Partition = blocks(block)(partition)

  override def blockCount: Int = runtimeConfig.blocking.enabledBlocks

  /**
   * The number of partitions in a specific block.
   */
  override def partitionCount(block: Int): Int = blocks(block).partitionCount

  private class Block(block: Int) {
    // Only full or sealed partitions are published here, so readers never see a partition still being written.
    @volatile
    private var partitions = Vector.empty[Partition]

    // The partition currently being filled; touched only by the writing thread.
    private var currentEntities = ArrayBuffer[Entity]()
    private var currentIndices = ArrayBuffer[BitsetIndex]()

    def apply(index: Int): Partition = partitions(index)

    def add(entity: Entity, index: BitsetIndex): Unit = {
      currentEntities.append(entity)
      currentIndices.append(index)
      if (currentEntities.size >= runtimeConfig.partitionSize) {
        seal()
      }
    }

    // Publishes the current partition as an immutable partition, if non-empty.
    def seal(): Unit = {
      if (currentEntities.nonEmpty) {
        partitions :+= Partition(currentEntities.toArray, currentIndices.toArray)
        currentEntities = ArrayBuffer[Entity]()
        currentIndices = ArrayBuffer[BitsetIndex]()
        logger.log(Level.FINE, "Written partition " + (partitions.size - 1) + " of block " + block)
      }
    }

    def partitionCount: Int = partitions.size
  }

}
