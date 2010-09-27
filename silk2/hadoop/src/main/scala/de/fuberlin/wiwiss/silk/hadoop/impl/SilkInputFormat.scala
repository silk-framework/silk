package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce._
import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.hadoop.SilkConfiguration
import java.io.{DataInput, DataOutput}
import org.apache.hadoop.io.{Writable, NullWritable}

class SilkInputFormat extends InputFormat[NullWritable, InstancePair]
{
    override def getSplits(jobContext : JobContext) : java.util.List[InputSplit] =
    {
        val config = SilkConfiguration.get(jobContext.getConfiguration)

        val inputSplits = new java.util.ArrayList[InputSplit]()

        for(blockIndex <- 0 until config.sourceCache.blockCount;
            is <- 0 until config.sourceCache.partitionCount(blockIndex);
            it <- 0 until config.targetCache.partitionCount(blockIndex))
        {

            //Get the list of nodes where both partitions of this split would be local if any.
            //If no host holds both partitions, returns the list of hosts which hold at least one partition.
            val sourceHosts = config.sourceCache.hostLocations(blockIndex, is)
            val targetHosts = config.targetCache.hostLocations(blockIndex, it)

            val unionHosts = sourceHosts union targetHosts
            val hosts =
                if(!unionHosts.isEmpty)
                {
                    unionHosts
                }
                else
                {
                    sourceHosts ++ targetHosts
                }

            //Compute the size of the split, so that the input splits can be sorted by size.
            val size = config.sourceCache.partitionSize(blockIndex, is) + config.targetCache.partitionSize(blockIndex, it)

            inputSplits.add(new SilkInputSplit(blockIndex, is, it, size, hosts))
        }

        inputSplits
    }

    override def createRecordReader(inputSplit : InputSplit, context : TaskAttemptContext) : RecordReader[NullWritable, InstancePair] =
    {
        new SilkRecordReader()
    }

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

    private class SilkRecordReader() extends RecordReader[NullWritable, InstancePair]
    {
        private var sourceInstances : Array[Instance] = null
        private var targetInstances : Array[Instance] = null

        private var sourceIndex = 0
        private var targetIndex = -1

        override def getProgress = (sourceIndex * targetInstances.length + targetIndex + 1).toFloat / (sourceInstances.length * targetInstances.length).toFloat

        override def initialize(inputSplit : InputSplit, context : TaskAttemptContext) : Unit =
        {
            val config = SilkConfiguration.get(context.getConfiguration)

            val silkInputSplit = inputSplit.asInstanceOf[SilkInputSplit]

            sourceInstances = config.sourceCache.read(silkInputSplit.blockIndex, silkInputSplit.sourcePartition)
            targetInstances = config.targetCache.read(silkInputSplit.blockIndex, silkInputSplit.targetPartition)

            context.setStatus("Comparing partition " + silkInputSplit.sourcePartition + " and " + silkInputSplit.targetPartition)
        }

        override def close : Unit =
        {
            sourceInstances = null
            targetInstances = null
        }

        override def nextKeyValue : Boolean =
        {
            if(sourceIndex == sourceInstances.length - 1 && targetIndex == targetInstances.length - 1)
            {
                false
            }
            else if(targetIndex == targetInstances.length - 1)
            {
                sourceIndex += 1
                targetIndex = 0

                true
            }
            else
            {
                targetIndex += 1

                true
            }
        }

        override def getCurrentKey = NullWritable.get

        override def getCurrentValue = new InstancePair(sourceInstances(sourceIndex), targetInstances(targetIndex))
    }
}
