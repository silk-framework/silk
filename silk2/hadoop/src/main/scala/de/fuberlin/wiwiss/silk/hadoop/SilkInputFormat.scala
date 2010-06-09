package de.fuberlin.wiwiss.silk.hadoop

import org.apache.hadoop.mapreduce._
import de.fuberlin.wiwiss.silk.Instance
import java.io.{DataInput, DataOutput}
import org.apache.hadoop.io.{NullWritable, Writable, Text}

class SilkInputFormat extends InputFormat[NullWritable, InstancePair]
{
    override def getSplits(jobContext : JobContext) : java.util.List[InputSplit] =
    {
        val inputSplits = new java.util.ArrayList[InputSplit]()

        for(is <- 0 until Silk.sourcePartitionCache.size;
            it <- 0 until Silk.targetPartitionCache.size)
        {
            inputSplits.add(new SilkInputSplit(is, it))
        }

        inputSplits
    }

    override def createRecordReader(inputSplit : InputSplit, context : TaskAttemptContext) : RecordReader[NullWritable, InstancePair] =
    {
        new SilkRecordReader()
    }
}

class SilkRecordReader extends RecordReader[NullWritable, InstancePair]
{
    private var sourceInstances : Array[Instance] = null
    private var targetInstances : Array[Instance] = null

    private var sourceIndex = 0
    private var targetIndex = -1

    private var lastProgress = -0.1f

    override def getProgress = (sourceIndex * targetInstances.length + targetIndex + 1).toFloat / (sourceInstances.length * targetInstances.length).toFloat

    override def initialize(inputSplit : InputSplit, context : TaskAttemptContext) : Unit =
    {
        val silkInputSplit = inputSplit.asInstanceOf[SilkInputSplit]

        sourceInstances = Silk.sourcePartitionCache(silkInputSplit.sourcePartition)
        targetInstances = Silk.targetPartitionCache(silkInputSplit.targetPartition)

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



