package de.fuberlin.wiwiss.silk

import config.Configuration
import de.fuberlin.wiwiss.silk.util.Task
import instance.InstanceCache
import linkspec.LinkSpecification
import collection.mutable.{SynchronizedBuffer, Buffer, ArrayBuffer}
import java.util.logging.{Level, Logger}
import output.Link
import java.util.concurrent._

/**
 * Executes the matching.
 * Generates links between the instances according to the link specification.
 */
class MatchTask(config : Configuration, linkSpec : LinkSpecification,
                sourceCache : InstanceCache, targetCache : InstanceCache,
                numThreads : Int) extends Task[Buffer[Link]]
{
  private val logger = Logger.getLogger(classOf[MatchTask].getName)

  private val linkBuffer = new ArrayBuffer[Link]() with SynchronizedBuffer[Link]

  def links : Buffer[Link] with SynchronizedBuffer[Link] = linkBuffer

  /**
   * Executes the matching.
   */
  override def execute() : Buffer[Link] =
  {
    require(sourceCache.blockCount == targetCache.blockCount, "sourceCache.blockCount == targetCache.blockCount")

    val startTime = System.currentTimeMillis()
    val executorService = Executors.newFixedThreadPool(numThreads)
    val executor = new ExecutorCompletionService[Traversable[Link]](executorService)

    //Start matching thread scheduler
    val scheduler = new SchedulerThread(executor)
    scheduler.start()
    updateStatus("Loading", 0.0)

    //Process finished tasks
    var finishedTasks = 0
    while(scheduler.isAlive || finishedTasks < scheduler.taskCount)
    {
      val result = executor.poll(100, TimeUnit.MILLISECONDS)
      if(result != null)
      {
        links.appendAll(result.get)
        finishedTasks += 1

        val status = if(scheduler.isAlive) "Executing Matching (Still loading) (" else "Executing Matching ("
        updateStatus(status + finishedTasks + " tasks finished)", finishedTasks.toDouble / scheduler.taskCount)
      }
    }

    executorService.shutdown()
    logger.info("Executed matching in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")

    linkBuffer
  }

  /**
   * Monitors the instance caches and schedules new matching threads whenever a new partition has been loaded.
   */
  private class SchedulerThread(executor : CompletionService[Traversable[Link]]) extends Thread
  {
    @volatile var taskCount = 0

    private var sourcePartitions = new Array[Int](sourceCache.blockCount)
    private var targetPartitions = new Array[Int](targetCache.blockCount)

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
        newMatcher(block, sourcePartition, targetPartition)
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
        newMatcher(block, sourcePartition, targetPartition)
      }

      targetPartitions = newTargetPartitions
    }

    private def newMatcher(block : Int, sourcePartition : Int, targetPartition : Int)
    {
      executor.submit(new Matcher(taskCount + 1, block, sourcePartition, targetPartition))
      taskCount += 1
    }
  }

  /**
   * Matches the instances of two partitions.
   */
  private class Matcher(id : Int, blockIndex : Int, sourcePartitionIndex : Int, targetPartitionIndex : Int) extends Callable[Traversable[Link]]
  {
    override def call() : Traversable[Link] =
    {
      logger.info("Starting matcher " + id)

      var links = List[Link]()

      try
      {
        val sourceInstances = sourceCache.read(blockIndex, sourcePartitionIndex)
        val targetInstances = targetCache.read(blockIndex, targetPartitionIndex)

        val sourceIndexes = sourceInstances.map(linkSpec.condition.index)
        val targetIndexes = targetInstances.map(linkSpec.condition.index)

        for(s <- 0 until sourceInstances.size;
            t <- 0 until targetInstances.size;
            if sourceIndexes(s).exists(targetIndexes(t).contains(_)))
        {
          val sourceInstance = sourceInstances(s)
          val targetInstance = targetInstances(t)

          val confidence = linkSpec.condition(sourceInstance, targetInstance)

          if(confidence >= linkSpec.filter.threshold)
          {
            links ::= new Link(sourceInstance.uri, targetInstance.uri, confidence)
          }
        }
      }
      catch
      {
        case ex : Exception => logger.log(Level.WARNING, "Could not execute match task", ex)
      }

      logger.info("Matcher " + id + " generated " + links.size + " links")

      links
    }
  }
}
