package de.fuberlin.wiwiss.silk.hadoop.impl

import java.util.logging.Logger
import org.apache.hadoop.fs.{Path, FileSystem}
import de.fuberlin.wiwiss.silk.instance._
import java.io._
import de.fuberlin.wiwiss.silk.config.RuntimeConfig

/**
 * An instance cache, which uses the Hadoop FileSystem API.
 * This can be used to cache the instances on any file system which is supported by Hadoop e.g. the Hadoop Distributed FileSystem.
 */
class HadoopInstanceCache(val instanceSpec: InstanceSpecification, fs: FileSystem, path: Path, runtimeConfig: RuntimeConfig) extends InstanceCache {

  private val logger = Logger.getLogger(getClass.getName)

  private val blocks = (for (i <- 0 until blockCount) yield new BlockReader(i)).toArray

  @volatile private var writing = false

  override def write(instances: Traversable[Instance], indexFunction: Instance => Set[Int]) {
    writing = true

    try {
      fs.delete(path, true)

      val blockWriters = (for (i <- 0 until blockCount) yield new BlockWriter(i)).toArray
      var instanceCount = 0

      for (instance <- instances) {
        val index = if(runtimeConfig.blocking.isEnabled) indexFunction(instance) else Set(0)

        for (block <- index.map(_ % blockCount)) {
          if (block < 0 || block >= blockCount) throw new IllegalArgumentException("Invalid blocking function. (Allocated Block: " + block + ")")

          blockWriters(block).write(instance, Index.build(index))
        }

        instanceCount += 1
      }

      blockWriters.foreach(_.close())

      blocks.foreach(_.reload())

      logger.info("Written " + instanceCount + " instances.")
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
      val stream = new DataInputStream(fs.open(blockPath.suffix("/partition" + partition)))

      try {
        val count = stream.readInt()
        val instances = new Array[Instance](count)
        val indices = new Array[Index](count)

        for (i <- 0 until count) {
          instances(i) = Instance.deserialize(stream, instanceSpec)
          indices(i) = Index.deserialize(stream)
        }

        Partition(instances, indices)
      }
      finally {
        stream.close()
      }
    }
  }

  private class BlockWriter(block: Int) {
    private var instances = new Array[Instance](runtimeConfig.partitionSize)
    private var indices = new Array[Index](runtimeConfig.partitionSize)
    private var count = 0

    private val blockPath = path.suffix("/block" + block + "/")
    fs.mkdirs(blockPath)

    private var partitionCount = 0

    def write(instance: Instance, index: Index) {
      instances(count) = instance
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
      instances = null
      indices = null
    }

    private def writePartition() {
      val stream = new DataOutputStream(fs.create(blockPath.suffix("/partition" + partitionCount)))

      try {
        stream.writeInt(count)
        for (i <- 0 until count) {
          instances(i).serialize(stream)
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