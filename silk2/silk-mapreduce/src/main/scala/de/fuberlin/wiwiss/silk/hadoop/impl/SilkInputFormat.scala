package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce._
import de.fuberlin.wiwiss.silk.hadoop.SilkConfiguration
import org.apache.hadoop.io.NullWritable
import de.fuberlin.wiwiss.silk.instance.Instance
import collection.immutable.HashSet

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

  private class SilkRecordReader() extends RecordReader[NullWritable, InstancePair]
  {
    private var sourceInstances : Array[Instance] = null
    private var targetInstances : Array[Instance] = null

    private var sourceIndices : Array[Set[Int]] = null
    private var targetIndices : Array[Set[Int]] = null

    private var sourceIndex = 0
    private var targetIndex = -1

    override def getProgress = (sourceIndex * targetInstances.length + targetIndex + 1).toFloat / (sourceInstances.length * targetInstances.length).toFloat

    override def initialize(inputSplit : InputSplit, context : TaskAttemptContext) : Unit =
    {
      val config = SilkConfiguration.get(context.getConfiguration)
      val linkSpec = config.linkSpec

      val silkInputSplit = inputSplit.asInstanceOf[SilkInputSplit]

      sourceInstances = config.sourceCache.read(silkInputSplit.blockIndex, silkInputSplit.sourcePartition)
      targetInstances = config.targetCache.read(silkInputSplit.blockIndex, silkInputSplit.targetPartition)

      sourceIndices = sourceInstances.map(instance => HashSet(linkSpec.condition.index(instance, 0.0).toSeq : _*))
      targetIndices = targetInstances.map(instance => HashSet(linkSpec.condition.index(instance, 0.0).toSeq : _*))

      context.setStatus("Comparing partition " + silkInputSplit.sourcePartition + " and " + silkInputSplit.targetPartition)
    }

    override def close : Unit =
    {
      sourceInstances = null
      targetInstances = null

      sourceIndices = null
      targetIndices = null
    }

    override def nextKeyValue : Boolean =
    {
      var result : Option[Boolean] = None
      while(!result.isDefined)
      {
        val hasNext =
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

        if(hasNext && sourceIndices(sourceIndex).exists(targetIndices(targetIndex).contains))
        {
          result = Some(true)
        }
        else if(!hasNext)
        {
          result = Some(false)
        }
      }
      result.get
    }

    override def getCurrentKey = NullWritable.get

    override def getCurrentValue = new InstancePair(sourceInstances(sourceIndex), targetInstances(targetIndex))
  }
}
