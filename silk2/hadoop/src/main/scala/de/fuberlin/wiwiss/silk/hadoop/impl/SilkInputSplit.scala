package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce._
import java.io.{DataInput, DataOutput}
import org.apache.hadoop.io.Writable

class SilkInputSplit(var blockIndex : Int, var sourcePartition : Int, var targetPartition : Int) extends InputSplit with Writable
{
    def this() = this(0, 0, 0)

    /**
     * Get the size of the split, so that the input splits can be sorted by size.
     * This methods always returns the same value as all inputs splits are approximately of the same size.
     */
    override def getLength() : Long = 0 //

    override def getLocations() : Array[String] = Array()

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
