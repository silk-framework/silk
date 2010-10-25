package de.fuberlin.wiwiss.silk.instance

import java.io._
import de.fuberlin.wiwiss.silk.util.FileUtils._
import java.util.logging.Logger

/**
 * An instance cache, which caches the instances on the local file system.
 */
class FileInstanceCache(dir : File, override val blockCount : Int = 1, maxPartitionSize : Int = 1000) extends InstanceCache
{
  require(blockCount >= 0, "blockCount must be greater than 0 (blockCount=" + blockCount + ")")
  require(maxPartitionSize >= 0, "maxPartitionSize must be greater than 0 (maxPartitionSize=" + maxPartitionSize + ")")

  private val logger = Logger.getLogger(getClass.getName)

  private val blocks = (for(i <- 0 until blockCount) yield new Block(i)).toArray

  @volatile private var writing = false

  override def write(instances : Traversable[Instance], blockingFunction : Option[Instance => Set[Int]] = None)
  {
    writing = true
    var instanceCount = 0

    try
    {
      for(instance <- instances)
      {
        for(block <- blockingFunction.map(f => f(instance)).getOrElse(Set(0)))
        {
          if(block < 0 || block >= blockCount) throw new IllegalArgumentException("Invalid blocking function. (Allocated Block: " + block + ")")

          blocks(block).write(instance)
        }

        instanceCount += 1
      }

      logger.info("Written " + instanceCount + " instances.")
    }
    finally
    {
      writing = false
    }
  }

  override def isWriting = writing

  override def read(block : Int, partition : Int) =
  {
    require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")
    require(partition >= 0 && partition < blocks(block).partitionCount, "0 <= partition < " + blocks(block).partitionCount + " (partition = " + partition + ")")

    blocks(block).read(partition)
  }

  override def partitionCount(block : Int) =
  {
    require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")

    blocks(block).partitionCount
  }

  override def clear()
  {
    for(block <- blocks)
    {
      block.clear()
    }
  }

  override def close()
  {
    for(block <- blocks)
    {
      block.close()
    }
  }

  private class Block(block : Int)
  {
    @volatile var partitionCount = 0

    private val blockDir = new File(dir + "/block" + block + "/")

    private val lastPartition = new Array[Instance](maxPartitionSize)
    @volatile private var lastPartitionSize = 0

    load()

    private def load()
    {
      blockDir.mkdirs()

      //Retrieve the number of existing partitions
      partitionCount =
      {
        if(blockDir.exists)
        {
          val partitionFiles = blockDir.list.filter(_.startsWith("partition")).map(name => name.dropWhile(!_.isDigit)).filter(!_.isEmpty)

          if(partitionFiles.isEmpty) 0
          else partitionFiles.map(_.toInt).max + 1
        }
        else
        {
          0
        }
      }

      //Load the last partition in memory
      if(partitionCount > 0)
      {
        val readPartition = readPartitionFromFile(partitionCount - 1)
        Array.copy(readPartition, 0, lastPartition, 0, readPartition.size)
      }
    }

    def read(partition : Int) : Array[Instance] =
    {
      if(partition == partitionCount - 1)
      {
        val partition = new Array[Instance](lastPartitionSize)
        Array.copy(lastPartition, 0, partition, 0, partition.size)
        partition
      }
      else
      {
        readPartitionFromFile(partition)
      }
    }

    def write(instance : Instance)
    {
      lastPartition(lastPartitionSize) = instance
      lastPartitionSize += 1

      if(partitionCount == 0)
      {
        partitionCount = 1
      }

      if(lastPartitionSize == maxPartitionSize)
      {
        writePartitionToFile()
        lastPartitionSize = 0
      }
    }

    def clear()
    {
      partitionCount = 0
      lastPartitionSize = 0
      blockDir.deleteRecursive()
      blockDir.mkdirs()
    }

    def close()
    {
      if(lastPartitionSize > 0)
      {
        writePartitionToFile()
      }
    }

    private def readPartitionFromFile(partition : Int) =
    {
      val stream = new ObjectInputStream(new FileInputStream(blockDir + "/partition" + partition))

      try
      {
        val partitionSize = stream.readInt()
        val partition = new Array[Instance](partitionSize)

        for(i <- 0 until partitionSize)
        {
          partition(i) = stream.readObject().asInstanceOf[Instance]
        }

        partition
      }
      finally
      {
        stream.close()
      }
    }

    private def writePartitionToFile()
    {
      val stream = new ObjectOutputStream(new FileOutputStream(blockDir + "/partition" + (partitionCount - 1)))

      try
      {
        stream.writeInt(lastPartitionSize)
        for(instance <- lastPartition)
        {
          stream.writeObject(instance)
        }
      }
      finally
      {
        stream.close()
      }

      logger.info("Written partition " + (partitionCount - 1) + " of block " + block)
      partitionCount += 1
    }
  }
}
