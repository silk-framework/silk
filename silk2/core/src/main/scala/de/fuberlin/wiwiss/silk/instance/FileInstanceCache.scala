package de.fuberlin.wiwiss.silk.instance

import java.io._
import de.fuberlin.wiwiss.silk.util.FileUtils._
import java.util.logging.Logger

/**
 * An instance cache, which caches the instance on the local file system.
 */
class FileInstanceCache(dir : File, val blockCount : Int = 1, maxPartitionSize : Int = 1000) extends InstanceCache
{
    require(blockCount >= 0, "blockCount must be greater than 0 (blockCount=" + blockCount + ")")
    require(maxPartitionSize >= 0, "maxPartitionSize must be greater than 0 (maxPartitionSize=" + maxPartitionSize + ")")

    private val logger = Logger.getLogger(getClass.getName)

    private val blocks = (for(i <- 0 until blockCount) yield new BlockReader(i)).toArray

    def write(instances : Traversable[Instance], blockingFunction : Option[Instance => Set[Int]] = None)
    {
        dir.deleteRecursive()

        val blockWriters = (for(i <- 0 until blockCount) yield new BlockWriter(i)).toArray
        var instanceCount = 0

        for(instance <- instances)
        {
            for(block <- blockingFunction.map(f => f(instance)).getOrElse(Set(0)))
            {
                if(block < 0 || block >= blockCount) throw new IllegalArgumentException("Invalid blocking function. (Allocated Block: " + block + ")")
    
                blockWriters(block).write(instance)
            }

            instanceCount += 1
        }

        blockWriters.foreach(_.close())

        blocks.foreach(_.reload())

        logger.info("Written " + instanceCount + " instances.")
    }

    def read(block : Int, partition : Int) =
    {
        require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")
        require(partition >= 0 && partition < blocks(block).partitionCount, "0 <= partition < " + blocks(block).partitionCount + " (partition = " + partition + ")")

        blocks(block).read(partition)
    }

    def partitionCount(block : Int) =
    {
        require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")

        blocks(block).partitionCount
    }

    private class BlockReader(block : Int)
    {
        private var blockDir = new File(dir + "/block" + block + "/")

        var partitionCount = 0

        reload()

        def reload()
        {
            partitionCount =
            {
                if(blockDir.exists)
                {
                    val partitionFiles = blockDir.list.map(name => name.dropWhile(!_.isDigit)).filter(!_.isEmpty)

                    if(partitionFiles.isEmpty) 0
                    else partitionFiles.map(_.toInt).max + 1
                }
                else
                {
                    0
                }
            }
        }

        def read(partition : Int) =
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
    }

    private class BlockWriter(block : Int)
    {
        private var instances = new Array[Instance](maxPartitionSize)
        private var instanceCount = 0

        private var blockDir = new File(dir + "/block" + block + "/")
        blockDir.mkdirs()

        private var partitionCount = 0

        def write(instance : Instance)
        {
            instances(instanceCount) = instance
            instanceCount += 1

            if(instanceCount == maxPartitionSize)
            {
                writePartition()
                instanceCount = 0
            }
        }

        def close()
        {
            if(instanceCount > 0)
            {
                writePartition()
            }
            instances = null
        }

        private def writePartition()
        {
            val stream = new ObjectOutputStream(new FileOutputStream(blockDir + "/partition" + partitionCount))

            try
            {
                stream.writeInt(instanceCount)
                for(instance <- instances)
                {
                    stream.writeObject(instance)
                }
            }
            finally
            {
                stream.close()
            }

            logger.info("Written partition " + partitionCount + " of block " + block)
            partitionCount += 1
        }
    }
}