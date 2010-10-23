package de.fuberlin.wiwiss.silk

import config.Configuration
import de.fuberlin.wiwiss.silk.workbench.Task
import instance.InstanceCache
import linkspec.LinkSpecification
import collection.mutable.{SynchronizedBuffer, Buffer, ArrayBuffer}
import java.util.logging.{Level, Logger}
import output.Link
import java.util.concurrent.{Executors, TimeUnit}

/**
 * Executes the matching.
 */
class MatchTask(config : Configuration, linkSpec : LinkSpecification,
                sourceCache : InstanceCache, targetCache : InstanceCache,
                numThreads : Int = Silk.DefaultThreads) extends Task[Unit]
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

    var links = generateLinks(sourceCache, targetCache)
    links = filterLinks(links)
    writeOutput(links)

    logger.info("Executed matching in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
  }

  /**
   * Generates links between the instances according to the link specification.
   */
  private def generateLinks(sourceCache : InstanceCache, targetCache : InstanceCache) : Buffer[Link] =
  {
    val executor = Executors.newFixedThreadPool(numThreads)
    val linkBuffer = new ArrayBuffer[Link]() with SynchronizedBuffer[Link]

    for(blockIndex <- 0 until sourceCache.blockCount;
        sourcePartitionIndex <- 0 until sourceCache.partitionCount(blockIndex);
        targetPartitionIndex <- 0 until targetCache.partitionCount(blockIndex))
    {
      executor.submit(new MatchingThread(sourceCache, targetCache, blockIndex, sourcePartitionIndex, targetPartitionIndex, link => linkBuffer.append(link)))
    }

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
   * A thread, which matches the instances of a single partition.
   */
  private class MatchingThread(sourceCache : InstanceCache, targetCache : InstanceCache, blockIndex : Int,
                          sourcePartitionIndex : Int, targetPartitionIndex : Int, callback : Link => Unit) extends Runnable
  {
    override def run() : Unit =
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

        logger.info("Completed match task " + taskNum + " of " + taskCount)
      }
      catch
      {
        case ex : Exception => logger.log(Level.WARNING, "Could not execute match task", ex)
      }
    }
  }
}
