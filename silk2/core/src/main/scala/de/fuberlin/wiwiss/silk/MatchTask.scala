package de.fuberlin.wiwiss.silk

import config.Configuration
import de.fuberlin.wiwiss.silk.workbench.Task
import instance.InstanceCache
import linkspec.LinkSpecification
import collection.mutable.{SynchronizedBuffer, Buffer, ArrayBuffer}
import java.util.logging.{Level, Logger}
import output.Link
import java.util.concurrent. {ExecutorService, Executors, TimeUnit}

/**
 * Executes the matching.
 * Generates links between the instances according to the link specification.
 */
class MatchTask(config : Configuration, linkSpec : LinkSpecification,
                sourceCache : InstanceCache, targetCache : InstanceCache,
                numThreads : Int = Silk.DefaultThreads) extends Task[Buffer[Link]]
{
  private val logger = Logger.getLogger(classOf[MatchTask].getName)

  /**
   * Executes the matching.
   */
  override def execute() : Buffer[Link] =
  {
    require(sourceCache.blockCount == targetCache.blockCount, "sourceCache.blockCount == targetCache.blockCount")

    logger.info("Starting matching")

    val startTime = System.currentTimeMillis()
    val executor = Executors.newFixedThreadPool(numThreads)
    val linkBuffer = new ArrayBuffer[Link]() with SynchronizedBuffer[Link]

    //Start matching thread scheduler
    val schedulerThread = new Thread(new SchedulerThread(executor, link => linkBuffer.append(link)))
    schedulerThread.start()

    //Await termination of the matching tasks
    schedulerThread.join()
    executor.shutdown()
    executor.awaitTermination(1000, TimeUnit.DAYS)

    logger.info("Executed matching in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")

    linkBuffer
  }

  /**
   * Monitors the instance caches and schedules new matching threads whenever a new partition has been loaded.
   */
  private class SchedulerThread(executor : ExecutorService, callback : Link => Unit) extends Runnable
  {
    var sourcePartitions = new Array[Int](sourceCache.blockCount)
    var targetPartitions = new Array[Int](targetCache.blockCount)

    override def run()
    {
      while(true)
      {
        val sourceLoaded = !sourceCache.isWriting
        val targetLoaded = !targetCache.isWriting

        updateSourcePartitions(sourceLoaded)
        updateTargetPartitions(targetLoaded)

        if(sourceLoaded && targetLoaded)
        {
          return
        }

        Thread.sleep(1000)
      }
    }

    private def updateSourcePartitions(includeLastPartitions : Boolean)
    {
      val newSourcePartitions =
      {
        for(block <- 0 until sourceCache.blockCount) yield
        {
          if(includeLastPartitions)
          {
            sourceCache.partitionCount(block)
          }
          else
          {
            Math.max(0, sourceCache.partitionCount(block) - 1)
          }
        }
      }.toArray

      for(block <- 0 until sourceCache.blockCount;
          sourcePartition <- sourcePartitions(block) until newSourcePartitions(block);
          targetPartition <- 0 until targetPartitions(block))
      {
         executor.submit(new MatchingThread(block, sourcePartition, targetPartition, callback))
      }

      sourcePartitions = newSourcePartitions
    }

    private def updateTargetPartitions(includeLastPartitions : Boolean)
    {
      val newTargetPartitions =
      {
        for(block <- 0 until targetCache.blockCount) yield
        {
          if(includeLastPartitions)
          {
            targetCache.partitionCount(block)
          }
          else
          {
            Math.max(0, targetCache.partitionCount(block) - 1)
          }
        }
      }.toArray

      for(block <- 0 until targetCache.blockCount;
          targetPartition <- targetPartitions(block) until newTargetPartitions(block);
          sourcePartition <- 0 until sourcePartitions(block))
      {
         executor.submit(new MatchingThread(block, sourcePartition, targetPartition, callback))
      }

      targetPartitions = newTargetPartitions
    }
  }

  /**
   * A thread, which matches the instances of two partitions.
   */
  private class MatchingThread(blockIndex : Int, sourcePartitionIndex : Int, targetPartitionIndex : Int,
                               callback : Link => Unit) extends Runnable
  {
    override def run()
    {
      try
      {
        val tasksPerBlock = for(block <- 0 until sourceCache.blockCount) yield sourceCache.partitionCount(block) * targetCache.partitionCount(block)
        val taskNum = tasksPerBlock.take(blockIndex).foldLeft(sourcePartitionIndex * targetCache.partitionCount(blockIndex) + targetPartitionIndex + 1)(_ + _)
        val taskCount = tasksPerBlock.reduceLeft(_ + _)

        logger.info("Starting match task " + taskNum + " of " + taskCount)

        for(sourceInstance <- sourceCache.read(blockIndex, sourcePartitionIndex);
            targetInstance <- targetCache.read(blockIndex, targetPartitionIndex))
        {
          val confidence = linkSpec.condition(sourceInstance, targetInstance)

          if(confidence >= linkSpec.filter.threshold)
          {
            callback(new Link(sourceInstance.uri, targetInstance.uri, confidence))
          }
        }

        logger.info("Completed match task " + taskNum)
      }
      catch
      {
        case ex : Exception => logger.log(Level.WARNING, "Could not execute match task", ex)
      }
    }
  }
}
