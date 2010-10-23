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
 */
class MatchTask(config : Configuration, linkSpec : LinkSpecification,
                sourceCache : InstanceCache, targetCache : InstanceCache,
                reload : Boolean = true, numThreads : Int = Silk.DefaultThreads) extends Task[Unit]
{
  private val logger = Logger.getLogger(classOf[MatchTask].getName)

  /**
   * Executes the matching.
   */
  override def execute()
  {
    require(sourceCache.blockCount == targetCache.blockCount, "sourceCache.blockCount == targetCache.blockCount")

    val startTime = System.currentTimeMillis()
    logger.info("Starting matching")

    var links = generateLinks()
    links = filterLinks(links)
    writeOutput(links)

    logger.info("Executed matching in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
  }

  /**
   * Generates links between the instances according to the link specification.
   */
  private def generateLinks() : Buffer[Link] =
  {
    val executor = Executors.newFixedThreadPool(numThreads)
    val linkBuffer = new ArrayBuffer[Link]() with SynchronizedBuffer[Link]

    //Load instances into cache
    val loadSourceCacheTask = new LoadTask(config, linkSpec, Some(sourceCache), None)
    val loadTargetCacheTask = new LoadTask(config, linkSpec, None, Some(targetCache))
    if(reload)
    {
      loadSourceCacheTask.runInBackground()
      loadTargetCacheTask.runInBackground()
    }

    //Start matching thread scheduler
    val schedulerThread = new Thread(new SchedulerThread(executor, link => linkBuffer.append(link),
                                                         () => loadSourceCacheTask.isRunning,
                                                         () => loadTargetCacheTask.isRunning))
    schedulerThread.start()

    //Await termination of the matching tasks
    schedulerThread.join()
    executor.shutdown()
    executor.awaitTermination(1000, TimeUnit.DAYS)

    linkBuffer
  }

  /**
   * Filters the links according to the link limit.
   */
  private def filterLinks(links : Buffer[Link]) : Buffer[Link] =
  {
    linkSpec.filter.limit match
    {
      case Some(limit) =>
      {
        val linkBuffer = new ArrayBuffer[Link]()
        logger.info("Filtering output")

        for((sourceUri, groupedLinks) <- links.groupBy(_.sourceUri))
        {
          val bestLinks = groupedLinks.sortWith(_.confidence > _.confidence).take(limit)

          linkBuffer.appendAll(bestLinks)
        }

        linkBuffer
      }
      case None => links
    }
  }

  /**
   * Writes the links to the output.
   */
  private def writeOutput(linkBuffer : Buffer[Link]) =
  {
    val outputs = config.outputs ++ linkSpec.outputs

    outputs.foreach(_.open)

    for(link <- linkBuffer;
        output <- outputs)
    {
      output.write(link, linkSpec.linkType)
    }

    outputs.foreach(_.close)
  }

  /**
   * Monitors the instance caches and schedules new matching threads whenever a new partition has been loaded.
   */
  private class SchedulerThread(executor : ExecutorService, callback : Link => Unit,
                                loadingSourceCache : () => Boolean, loadingTargetCache : () => Boolean) extends Runnable
  {
    var sourcePartitions = new Array[Int](sourceCache.blockCount)
    var targetPartitions = new Array[Int](targetCache.blockCount)

    override def run()
    {
      while(true)
      {
        val sourceLoaded = !loadingSourceCache()
        val targetLoaded = !loadingTargetCache()

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
