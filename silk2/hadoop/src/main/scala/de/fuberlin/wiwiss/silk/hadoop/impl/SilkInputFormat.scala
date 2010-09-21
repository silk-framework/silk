package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce._
import de.fuberlin.wiwiss.silk.instance.Instance
import org.apache.hadoop.io.NullWritable
import de.fuberlin.wiwiss.silk.hadoop.Silk

class SilkInputFormat extends InputFormat[NullWritable, InstancePair]
{
    override def getSplits(jobContext : JobContext) : java.util.List[InputSplit] =
    {
        val inputSplits = new java.util.ArrayList[InputSplit]()

        for(blockIndex <- 0 until Silk.numBlocks;
            is <- 0 until Silk.sourceCache.partitionCount(blockIndex);
            it <- 0 until Silk.targetCache.partitionCount(blockIndex))
        {
            inputSplits.add(new SilkInputSplit(blockIndex, is, it))
        }

        inputSplits
    }

    override def createRecordReader(inputSplit : InputSplit, context : TaskAttemptContext) : RecordReader[NullWritable, InstancePair] =
    {
        new SilkRecordReader()
    }

    private class SilkRecordReader extends RecordReader[NullWritable, InstancePair]
    {
        private var sourceInstances : Array[Instance] = null
        private var targetInstances : Array[Instance] = null

        private var sourceIndex = 0
        private var targetIndex = -1

        override def getProgress = (sourceIndex * targetInstances.length + targetIndex + 1).toFloat / (sourceInstances.length * targetInstances.length).toFloat

        override def initialize(inputSplit : InputSplit, context : TaskAttemptContext) : Unit =
        {
            val silkInputSplit = inputSplit.asInstanceOf[SilkInputSplit]

            sourceInstances = Silk.sourceCache.read(silkInputSplit.blockIndex, silkInputSplit.sourcePartition)
            targetInstances = Silk.targetCache.read(silkInputSplit.blockIndex, silkInputSplit.targetPartition)

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