package de.fuberlin.wiwiss.silk.hadoop

import org.apache.hadoop.mapreduce._
import de.fuberlin.wiwiss.silk.Instance
import java.io.{DataInput, DataOutput}
import org.apache.hadoop.io.{NullWritable, Writable, Text}


class SilkInputSplit(var sourcePartition : Int, var targetPartition : Int) extends InputSplit with Writable
{
    def this() = this(0,0)

    override def getLength() : Long = 0

    override def getLocations() : Array[String] = Array()

    override def write(out : DataOutput) : Unit =
    {
        out.writeInt(sourcePartition)
        out.writeInt(targetPartition)
    }

    override def readFields(in : DataInput) : Unit =
    {
        sourcePartition = in.readInt()
        targetPartition = in.readInt()
    }
}

