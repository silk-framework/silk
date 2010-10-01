package de.fuberlin.wiwiss.silk.hadoop.impl

import java.io._
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.instance.{Instance, InstanceCache}
import org.apache.hadoop.fs.{Path, FileSystem}

/**
 * An instance cache, which uses the Hadoop FileSystem API.
 * This can be used to cache the instances on any file system which is supported by Hadoop e.g. the Hadoop Distributed FileSystem.
 */
class HadoopInstanceCache(fs : FileSystem, path : Path, val blockCount : Int = 1, maxPartitionSize : Int = 1000) extends InstanceCache
{
    require(blockCount >= 0, "blockCount must be greater than 0 (blockCount=" + blockCount + ")")
    require(maxPartitionSize >= 0, "maxPartitionSize must be greater than 0 (maxPartitionSize=" + maxPartitionSize + ")")

    private val logger = Logger.getLogger(getClass.getName)

    private val blocks = (for(i <- 0 until blockCount) yield new BlockReader(i)).toArray

    override def write(instances : Traversable[Instance], blockingFunction : Option[Instance => Set[Int]] = None)
    {
        fs.delete(path, true)

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

    /**
     * The size of a specific partition.
     */
    def partitionSize(block : Int, partition : Int) : Long =
    {
        require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")
        require(partition >= 0 && partition < blocks(block).partitionCount, "0 <= partition < " + blocks(block).partitionCount + " (partition = " + partition + ")")

        fs.getFileStatus(path.suffix("/block" + block + "/partition" + partition)).getLen
    }

    /**
     * The list of nodes by name where the partition would be local.
     */
    def hostLocations(block : Int, partition : Int) : Array[String] =
    {
        require(block >= 0 && block < blockCount, "0 <= block < " + blockCount + " (block = " + block + ")")
        require(partition >= 0 && partition < blocks(block).partitionCount, "0 <= partition < " + blocks(block).partitionCount + " (partition = " + partition + ")")

        val file = fs.getFileStatus(path.suffix("/block" + block + "/partition" + partition))
        fs.getFileBlockLocations(file, 0, file.getLen).flatMap(_.getHosts)
    }

    private class BlockReader(block : Int)
    {
        private val blockPath = path.suffix("/block" + block + "/")

        @volatile private var partitionCountCache = -1

        def partitionCount =
        {
            if(partitionCountCache == -1)
            {
                partitionCountCache =
                {
                    if(fs.exists(blockPath))
                    {
                        val partitionFiles = fs.listStatus(blockPath)
                                               .filter(_.getPath.getName.startsWith("partition"))
                                               .map(_.getPath.getName.dropWhile(!_.isDigit))
                                               .filter(!_.isEmpty)

                        if(partitionFiles.isEmpty) 0
                        else partitionFiles.map(_.toInt).max + 1
                    }
                    else
                    {
                        0
                    }
                }
            }

            partitionCountCache
        }

        def reload()
        {
            partitionCountCache = -1
        }

        def read(partition : Int) : Array[Instance] =
        {
            val stream = new ObjectInputStream(fs.open(blockPath.suffix("/partition" + partition)))

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

        private val blockPath = path.suffix("/block" + block + "/")
        fs.mkdirs(blockPath)

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
            val stream = new ObjectOutputStream(fs.create(blockPath.suffix("/partition" + partitionCount)))

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