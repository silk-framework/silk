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

package de.fuberlin.wiwiss.silk.hadoop.impl

import java.util.logging.Logger
import org.apache.hadoop.fs.{Path, FileSystem}
import de.fuberlin.wiwiss.silk.entity._
import java.io._
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import de.fuberlin.wiwiss.silk.cache.{BitsetIndex, Partition, EntityCache}

/**
 * An entity cache, which uses the Hadoop FileSystem API.
 * This can be used to cache the entities on any file system which is supported by Hadoop e.g. the Hadoop Distributed FileSystem.
 */
class HadoopEntityCache(val entityDesc: EntityDescription,
                        val indexFunction: (Entity => Index),
                        fs: FileSystem, path: Path,
                        runtimeConfig: RuntimeConfig) extends EntityCache {

  private val logger = Logger.getLogger(getClass.getName)

  private val blocks = (for (i <- 0 until blockCount) yield new BlockReader(i)).toArray

  @volatile private var writing = false

  override def write(entities: Traversable[Entity]) {
    writing = true

    try {
      fs.delete(path, true)

      val blockWriters = (for (i <- 0 until blockCount) yield new BlockWriter(i)).toArray
      var entityCount = 0

      for (entity <- entities) {
        val indices = if(runtimeConfig.blocking.isEnabled) indexFunction(entity).flatten else Set(0)

        for ((block, index) <- indices.groupBy(i => math.abs(i % blockCount))) {
          blockWriters(block).write(entity, BitsetIndex.build(index))
        }

        entityCount += 1
      }

      blockWriters.foreach(_.close())

      blocks.foreach(_.reload())

      logger.info("Written " + entityCount + " entities.")
    }
    finally {
      writing = false
    }
  }

  override def isWriting = writing

  override def read(block: Int, partition: Int) = {
    require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")
    require(partition >= 0 && partition < blocks(block).partitionCount, "0 <= partition < " + blocks(block).partitionCount + " (partition = " + partition + ")")

    blocks(block).read(partition)
  }

  override def clear() {
    //throw new UnsupportedOperationException()
  }

  override def close() { }

  override def blockCount: Int = runtimeConfig.blocking.enabledBlocks

  override def partitionCount(block: Int) = {
    require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")

    blocks(block).partitionCount
  }

  /**
   * The size of a specific partition.
   */
  def partitionSize(block: Int, partition: Int): Long = {
    require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")
    require(partition >= 0 && partition < blocks(block).partitionCount, "0 <= partition < " + blocks(block).partitionCount + " (partition = " + partition + ")")

    fs.getFileStatus(path.suffix("/block" + block + "/partition" + partition)).getLen
  }

  /**
   * The list of nodes by name where the partition would be local.
   */
  def hostLocations(block: Int, partition: Int): Array[String] = {
    require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")
    require(partition >= 0 && partition < blocks(block).partitionCount, "0 <= partition < " + blocks(block).partitionCount + " (partition = " + partition + ")")

    val file = fs.getFileStatus(path.suffix("/block" + block + "/partition" + partition))
    fs.getFileBlockLocations(file, 0, file.getLen).flatMap(_.getHosts)
  }

  private class BlockReader(block: Int) {
    private val blockPath = path.suffix("/block" + block + "/")

    @volatile private var partitionCountCache = -1

    def partitionCount = {
      if (partitionCountCache == -1) {
        partitionCountCache = {
          if (fs.exists(blockPath)) {
            val partitionFiles = fs.listStatus(blockPath)
                                   .filter(_.getPath.getName.startsWith("partition"))
                                   .map(_.getPath.getName.dropWhile(!_.isDigit))
                                   .filter(!_.isEmpty)

            if (partitionFiles.isEmpty) 0
            else partitionFiles.map(_.toInt).max + 1
          } else {
            0
          }
        }
      }

      partitionCountCache
    }

    def reload() {
      partitionCountCache = -1
    }

    def read(partition: Int): Partition = {
      val stream = new DataInputStream(new BufferedInputStream(fs.open(blockPath.suffix("/partition" + partition))))

      try {
        val count = stream.readInt()
        val entities = new Array[Entity](count)
        val indices = new Array[BitsetIndex](count)

        for (i <- 0 until count) {
          entities(i) = Entity.deserialize(stream, entityDesc)
          indices(i) = BitsetIndex.deserialize(stream)
        }

        Partition(entities, indices)
      }
      finally {
        stream.close()
      }
    }
  }

  private class BlockWriter(block: Int) {
    private var entities = new Array[Entity](runtimeConfig.partitionSize)
    private var indices = new Array[BitsetIndex](runtimeConfig.partitionSize)
    private var count = 0

    private val blockPath = path.suffix("/block" + block + "/")
    fs.mkdirs(blockPath)

    private var partitionCount = 0

    def write(entity: Entity, index: BitsetIndex) {
      entities(count) = entity
      indices(count) = index
      count += 1

      if (count == runtimeConfig.partitionSize) {
        writePartition()
        count = 0
      }
    }

    def close() {
      if (count > 0) {
        writePartition()
      }
      entities = null
      indices = null
    }

    private def writePartition() {
      val stream = new DataOutputStream(new BufferedOutputStream(fs.create(blockPath.suffix("/partition" + partitionCount))))

      try {
        stream.writeInt(count)
        for (i <- 0 until count) {
          entities(i).serialize(stream)
          indices(i).serialize(stream)
        }
      }
      finally {
        stream.close()
      }

      logger.info("Written partition " + partitionCount + " of block " + block)
      partitionCount += 1
    }
  }

}