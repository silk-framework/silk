package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.Instance
import de.fuberlin.wiwiss.silk.util.FileUtils._
import java.io._

/**
 * A partition cache, which caches the partitions on the local file system.
 */
class FilePartitionCache(dir : File) extends PartitionCache
{
    private val maxPartitionSize = 1000

    @volatile
    private var _partitionCount =
    {
        if(dir.exists)
        {
            val partitionFiles = dir.list.map(name => name.dropWhile(!_.isDigit)).filter(!_.isEmpty)
            if(partitionFiles.isEmpty)
            {
                0
            }
            else
            {
                partitionFiles.map(_.toInt).max + 1
            }
        }
        else
        {
            0
        }
    }

    @volatile
    private var _isWriting = false

    override def size = _partitionCount

    override def isWriting = _isWriting

    override def apply(index : Int) : Array[Instance] =
    {
        val stream = new ObjectInputStream(new FileInputStream(dir + "/cluster" + index))

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

    override def write(instances : Traversable[Instance])
    {
        _isWriting = true

        dir.deleteRecursive()
        dir.mkdirs()

        var curPartition = List[Instance]()
        var curPartitionSize = 0
        _partitionCount = 0

        for(instance <- instances)
        {
            curPartition ::= instance
            curPartitionSize += 1

            if(curPartitionSize == maxPartitionSize)
            {
                writePartition(curPartition)

                curPartition = List[Instance]()
                curPartitionSize = 0
                _partitionCount += 1
            }
        }

        _isWriting = false
    }

    private def writePartition(instances : List[Instance])
    {
        val stream = new ObjectOutputStream(new FileOutputStream(dir + "/partition" + _partitionCount))

        try
        {
            stream.writeInt(maxPartitionSize)
            for(instance <- instances)
            {
                stream.writeObject(instance)
            }
        }
        finally
        {
            stream.close()
        }

        println("Written partition " + _partitionCount)
    }
}
