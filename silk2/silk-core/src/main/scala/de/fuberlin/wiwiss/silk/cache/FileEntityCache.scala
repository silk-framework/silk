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

package de.fuberlin.wiwiss.silk.cache

import de.fuberlin.wiwiss.silk.util.FileUtils._
import java.util.logging.{Level, Logger}
import java.io._
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import de.fuberlin.wiwiss.silk.entity.{Index, Entity, EntityDescription}

/**
 * An entity cache, which caches the entities on the local file system.
 */
class FileEntityCache(val entityDesc: EntityDescription,
                      val indexFunction: (Entity => Index),
                      dir: File,
                      runtimeConfig: RuntimeConfig = RuntimeConfig()) extends EntityCache {

  private val logger = Logger.getLogger(getClass.getName)

  private val logLevel = Level.FINE

  private val blocks = (for (i <- 0 until blockCount) yield new Block(i)).toArray

  @volatile private var writing = false

  override def write(entities: Traversable[Entity]) {
    val startTime = System.currentTimeMillis()
    writing = true
    var entityCount = 0

    try {
      for (entity <- entities) {
        if(Thread.currentThread().isInterrupted) throw new InterruptedException()

        val indices = if(runtimeConfig.blocking.isEnabled) indexFunction(entity).flatten else Set(0)

        for ((block, index) <- indices.groupBy(i => math.abs(i % blockCount))) {
          blocks(block).write(entity, BitsetIndex.build(index))
        }

        if (!indices.isEmpty) entityCount += 1
      }

      val time = ((System.currentTimeMillis - startTime) / 1000.0)
      logger.log(logLevel, "Finished writing " + entityCount + " entities with type '" + entityDesc.restrictions + "' in " + time + " seconds")
    } finally {
      writing = false
    }
  }

  override def isWriting = writing

  override def read(block: Int, partition: Int) = {
    require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")
    require(partition >= 0 && partition < blocks(block).partitionCount, "0 <= partition < " + blocks(block).partitionCount + " (partition = " + partition + ")")

    blocks(block).read(partition)
  }

  override def blockCount: Int = runtimeConfig.blocking.enabledBlocks

  override def partitionCount(block: Int) = {
    require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")

    blocks(block).partitionCount
  }

  override def clear() {
    dir.deleteRecursive()
    for (block <- blocks) {
      block.clear()
    }
  }

  override def close() {
    for (block <- blocks) {
      block.close()
    }
  }

  private class Block(block: Int) {
    @volatile var partitionCount = 0

    private val blockDir = dir + "/block" + block.toString + "/"

    private val currentEntities = new Array[Entity](runtimeConfig.partitionSize)
    private val currentIndices = new Array[BitsetIndex](runtimeConfig.partitionSize)
    @volatile private var count = 0

    if (runtimeConfig.reloadCache)
      clear()
    else
      load()

    private def load() {
      //Retrieve the number of existing partitions
      partitionCount = {
        if (blockDir.exists) {
          val partitionFiles = blockDir.list.filter(_.startsWith("partition")).map(name => name.dropWhile(!_.isDigit)).filter(!_.isEmpty)

          if (partitionFiles.isEmpty) 0
          else partitionFiles.map(_.toInt).max + 1
        }
        else {
          0
        }
      }

      //Load the last partition in memory
      if (partitionCount > 0) {
        val readPartition = readPartitionFromFile(partitionCount - 1)
        Array.copy(readPartition.entities, 0, currentEntities, 0, readPartition.size)
        Array.copy(readPartition.indices, 0, currentIndices, 0, readPartition.size)
        count = readPartition.size
      }
    }

    def read(partitionIndex: Int): Partition = {
      if (partitionIndex == partitionCount - 1) {
        Partition(currentEntities, currentIndices, count)
      }
      else {
        readPartitionFromFile(partitionIndex)
      }
    }

    def write(entity: Entity, index: BitsetIndex) {
      if (partitionCount == 0) partitionCount = 1

      currentEntities(count) = entity
      currentIndices(count) = index
      count += 1

      if (count == runtimeConfig.partitionSize) {
        writePartitionToFile()
        count = 0
        partitionCount += 1
      }
    }

    def clear() {
      partitionCount = 0
      count = 0
    }

    def close() {
      if (count > 0) {
        writePartitionToFile()
      }
    }

    private def readPartitionFromFile(partition: Int) = {
      val stream = new DataInputStream(new BufferedInputStream(new FileInputStream(blockDir + "/partition" + partition.toString)))

      try {
        Partition.deserialize(stream, entityDesc)
      }
      finally {
        stream.close()
      }
    }

    private def writePartitionToFile() {
      if (partitionCount == 1) blockDir.mkdirs()

      val stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(blockDir + "/partition" + (partitionCount - 1).toString)))

      try {
        Partition(currentEntities, currentIndices, count).serialize(stream)
      }
      finally {
        stream.close()
      }

      logger.log(logLevel, "Written partition " + (partitionCount - 1) + " of block " + block)
    }
  }

}
