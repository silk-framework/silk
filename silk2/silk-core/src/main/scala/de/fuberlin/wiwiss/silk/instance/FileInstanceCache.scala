package de.fuberlin.wiwiss.silk.instance

import de.fuberlin.wiwiss.silk.util.FileUtils._
import java.util.logging.Logger
import java.io._

/**
 * An instance cache, which caches the instances on the local file system.
 */
class FileInstanceCache(instanceSpec: InstanceSpecification, dir: File, clearOnLoading: Boolean = false, override val blockCount: Int = 1, maxPartitionSize: Int = 1000) extends InstanceCache {
  require(blockCount >= 0, "blockCount must be greater than 0 (blockCount=" + blockCount + ")")
  require(maxPartitionSize >= 0, "maxPartitionSize must be greater than 0 (maxPartitionSize=" + maxPartitionSize + ")")

  private val logger = Logger.getLogger(getClass.getName)

  private val blocks = (for (i <- 0 until blockCount) yield new Block(i)).toArray

  @volatile private var writing = false

  override def write(instances: Traversable[Instance], indexFunction: Option[Instance => Set[Int]] = None) {
    val startTime = System.currentTimeMillis()
    writing = true
    var instanceCount = 0

    try {
      for (instance <- instances) {
        val index = indexFunction.map(f => f(instance)).getOrElse(Set(0))

        for (block <- index.map(_ % blockCount)) {
          if (block < 0 || block >= blockCount) throw new IllegalArgumentException("Invalid blocking function. (Allocated Block: " + block + ")")

          blocks(block).write(instance, Index.build(index))
        }

        if (!index.isEmpty) instanceCount += 1
      }

      val time = ((System.currentTimeMillis - startTime) / 1000.0)
      logger.info("Finished writing " + instanceCount + " instances with type '" + instanceSpec.restrictions + "' in " + time + " seconds")
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

  override def partitionCount(block: Int) = {
    require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")

    blocks(block).partitionCount
  }

  override def clear() {
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

    private val currentInstances = new Array[Instance](maxPartitionSize)
    private val currentIndices = new Array[Index](maxPartitionSize)
    @volatile private var count = 0

    if (clearOnLoading)
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
        Array.copy(readPartition.instances, 0, currentInstances, 0, readPartition.size)
        Array.copy(readPartition.indices, 0, currentIndices, 0, readPartition.size)
        count = readPartition.size
      }
    }

    def read(partitionIndex: Int): Partition = {
      if (partitionIndex == partitionCount - 1) {
        Partition(currentInstances, currentIndices, count)
      }
      else {
        readPartitionFromFile(partitionIndex)
      }
    }

    def write(instance: Instance, index: Index) {
      if (partitionCount == 0) partitionCount = 1

      currentInstances(count) = instance
      currentIndices(count) = index
      count += 1

      if (count == maxPartitionSize) {
        writePartitionToFile()
        count = 0
        partitionCount += 1
      }
    }

    def clear() {
      partitionCount = 0
      count = 0
      //TODO execute deleteRecursive once on whole cache?
      blockDir.deleteRecursive()
    }

    def close() {
      if (count > 0) {
        writePartitionToFile()
      }
    }

    private def readPartitionFromFile(partition: Int) = {
      val stream = new DataInputStream(new FileInputStream(blockDir + "/partition" + partition.toString))

      try {
        Partition.deserialize(stream, instanceSpec)
      }
      finally {
        stream.close()
      }
    }

    private def writePartitionToFile() {
      if (partitionCount == 1) blockDir.mkdirs()

      val stream = new DataOutputStream(new FileOutputStream(blockDir + "/partition" + (partitionCount - 1).toString))

      try {
        Partition(currentInstances, currentIndices, count).serialize(stream)
      }
      finally {
        stream.close()
      }

      logger.info("Written partition " + (partitionCount - 1) + " of block " + block)
    }
  }

}
