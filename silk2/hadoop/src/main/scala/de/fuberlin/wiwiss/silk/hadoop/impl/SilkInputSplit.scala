package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce._
import java.io.{DataInput, DataOutput}
import org.apache.hadoop.io.Writable
import de.fuberlin.wiwiss.silk.hadoop.Silk

class SilkInputSplit(var blockIndex : Int, var sourcePartition : Int, var targetPartition : Int) extends InputSplit with Writable
{
    def this() = this(0, 0, 0)

    /**
     * Get the size of the split, so that the input splits can be sorted by size.
     */
    override def getLength() : Long = Silk.sourceCache.partitionSize(blockIndex, sourcePartition) + Silk.targetCache.partitionSize(blockIndex, targetPartition)

    /**
     * Get the list of nodes where both partitions of this split would be local if any.
     * If no host holds both partitions, returns the list of hosts which hold at least one partition.
     */
    override def getLocations() : Array[String] =
    {
        val sourceHosts = Silk.sourceCache.hostLocations(blockIndex, sourcePartition)
        val targetHosts = Silk.targetCache.hostLocations(blockIndex, targetPartition)

        val union = sourceHosts union targetHosts
        if(!union.isEmpty)
        {
            union
        }
        else
        {
            sourceHosts ++ targetHosts
        }
    }

    override def write(out : DataOutput) : Unit =
    {
        out.writeInt(blockIndex)
        out.writeInt(sourcePartition)
        out.writeInt(targetPartition)
    }

    override def readFields(in : DataInput) : Unit =
    {
        blockIndex = in.readInt()
        sourcePartition = in.readInt()
        targetPartition = in.readInt()
    }
}
