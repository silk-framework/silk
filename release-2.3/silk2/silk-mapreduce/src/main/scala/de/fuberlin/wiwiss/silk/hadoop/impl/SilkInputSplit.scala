package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce.InputSplit
import java.io.{DataInput, DataOutput}
import org.apache.hadoop.io.Writable

class SilkInputSplit(var blockIndex : Int, var sourcePartition : Int, var targetPartition : Int, var size : Long, var hosts : Array[String]) extends InputSplit with Writable
{
  def this() = this(0, 0, 0, 0, null)

  /**
   * Get the size of the split, so that the input splits can be sorted by size.
   */
  override def getLength() : Long = size

  /**
   * Get the list of nodes where both partitions of this split would be local if any.
   * If no host holds both partitions, returns the list of hosts which hold at least one partition.
   */
  override def getLocations() : Array[String] = hosts

  override def write(out : DataOutput) : Unit =
  {
    out.writeInt(blockIndex)
    out.writeInt(sourcePartition)
    out.writeInt(targetPartition)
    out.writeLong(size)
    out.writeInt(hosts.length)
    for(host <- hosts)
    {
      out.writeUTF(host)
    }
  }

  override def readFields(in : DataInput) : Unit =
  {
    blockIndex = in.readInt()
    sourcePartition = in.readInt()
    targetPartition = in.readInt()
    size = in.readLong()
    hosts = new Array[String](in.readInt())
    for(i <- 0 until hosts.length)
    {
      hosts(i) = in.readUTF()
    }
  }
}
