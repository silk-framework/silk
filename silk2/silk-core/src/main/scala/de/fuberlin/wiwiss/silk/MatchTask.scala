package de.fuberlin.wiwiss.silk

import instance.{Instance, InstanceCache}
import linkspec.evaluation.DetailedEvaluator
import linkspec.LinkSpecification
import java.util.logging.{Level, Logger}
import output.Link
import java.util.concurrent._
import collection.mutable.{SynchronizedBuffer, Buffer, ArrayBuffer}
import collection.immutable.HashSet
import util.{SourceTargetPair, Task}
import scala.math.{min, max}

/**
 * Executes the matching.
 * Generates links between the instances according to the link specification.
 */
class MatchTask(linkSpec : LinkSpecification,
                caches : SourceTargetPair[InstanceCache],
                numThreads : Int,
                sourceEqualsTarget : Boolean = false,
                generateDetailedLinks : Boolean = false) extends Task[Buffer[Link]]
{
  taskName = "Matching"

  private val logger = Logger.getLogger(classOf[MatchTask].getName)

  private val linkBuffer = new ArrayBuffer[Link]() with SynchronizedBuffer[Link]

  /* Enable indexing if blocking is enabled */
  private val indexingEnabled = caches.source.blockCount > 1 || caches.target.blockCount > 1

  @volatile private var cancelled = false

  def links : Buffer[Link] with SynchronizedBuffer[Link] = linkBuffer

  /**
   * Executes the matching.
   */
  override def execute() : Buffer[Link] =
  {
    require(caches.source.blockCount == caches.target.blockCount, "sourceCache.blockCount == targetCache.blockCount")

    //Reset properties
    linkBuffer.clear()
    cancelled = false

    //Create execution service for the matching tasks
    val startTime = System.currentTimeMillis()
    val executorService = Executors.newFixedThreadPool(numThreads)
    val executor = new ExecutorCompletionService[Traversable[Link]](executorService)

    //Start matching thread scheduler
    val scheduler = new SchedulerThread(executor)
    scheduler.start()

    //Process finished tasks
    var finishedTasks = 0
    while(!cancelled && (scheduler.isAlive || finishedTasks < scheduler.taskCount))
    {
      val result = executor.poll(100, TimeUnit.MILLISECONDS)
      if(result != null)
      {
        linkBuffer.appendAll(result.get)
        finishedTasks += 1

        //Update status
        val statusPrefix = if(scheduler.isAlive) "Matching (loading):" else "Matching:"
        val statusTasks = " " + finishedTasks + " tasks finished and"
        val statusLinks = " " + linkBuffer.size + " links generated."
        updateStatus(statusPrefix + statusTasks + statusLinks, finishedTasks.toDouble / scheduler.taskCount)
      }
    }

    //Shutdown
    if(scheduler.isAlive)
    {
      scheduler.interrupt()
    }
    if(cancelled)
    {
      executorService.shutdownNow()
    }
    else
    {
      executorService.shutdown()
    }

    //Log result
    val time = ((System.currentTimeMillis - startTime) / 1000.0) + " seconds"
    if(cancelled)
    {
      logger.info("Matching cancelled after " + time)
    }
    else
    {
      logger.info("Executed matching in " +  time)
    }

    linkBuffer
  }

  override def stopExecution()
  {
    cancelled = true
  }

  /**
   * Monitors the instance caches and schedules new matching threads whenever a new partition has been loaded.
   */
  private class SchedulerThread(executor : CompletionService[Traversable[Link]]) extends Thread
  {
    @volatile var taskCount = 0

    private var sourcePartitions = new Array[Int](caches.source.blockCount)
    private var targetPartitions = new Array[Int](caches.target.blockCount)

    override def run()
    {
      try
      {
        while(true)
        {
          val sourceLoaded = !caches.source.isWriting
          val targetLoaded = !caches.target.isWriting

          updateSourcePartitions(sourceLoaded)
          updateTargetPartitions(targetLoaded)

          if(sourceLoaded && targetLoaded)
          {
            return
          }

          Thread.sleep(1000)
        }
      }
      catch
      {
        case ex : InterruptedException =>
      }
    }

    private def updateSourcePartitions(includeLastPartitions : Boolean)
    {
      val newSourcePartitions =
      {
        for(block <- 0 until caches.source.blockCount) yield
        {
          if(includeLastPartitions)
          {
            caches.source.partitionCount(block)
          }
          else
          {
            max(0, caches.source.partitionCount(block) - 1)
          }
        }
      }.toArray

      for(block <- 0 until caches.source.blockCount;
          sourcePartition <- sourcePartitions(block) until newSourcePartitions(block);
          targetStart = if(!sourceEqualsTarget) 0 else sourcePartition;
          targetPartition <- targetStart until targetPartitions(block))
      {
        newMatcher(block, sourcePartition, targetPartition)
      }

      sourcePartitions = newSourcePartitions
    }

    private def updateTargetPartitions(includeLastPartitions : Boolean)
    {
      val newTargetPartitions =
      {
        for(block <- 0 until caches.target.blockCount) yield
        {
          if(includeLastPartitions)
          {
            caches.target.partitionCount(block)
          }
          else
          {
            max(0, caches.target.partitionCount(block) - 1)
          }
        }
      }.toArray

      for(block <- 0 until caches.target.blockCount;
          targetPartition <- targetPartitions(block) until newTargetPartitions(block);
          sourceEnd = if(!sourceEqualsTarget) sourcePartitions(block) else min(sourcePartitions(block), targetPartition + 1);
          sourcePartition <- 0 until sourceEnd)
      {
        newMatcher(block, sourcePartition, targetPartition)
      }

      targetPartitions = newTargetPartitions
    }

    private def newMatcher(block : Int, sourcePartition : Int, targetPartition : Int)
    {
      executor.submit(new Matcher(block, sourcePartition, targetPartition))
      taskCount += 1
    }
  }

  /**
   * Matches the instances of two partitions.
   */
  private class Matcher(blockIndex : Int, sourcePartitionIndex : Int, targetPartitionIndex : Int) extends Callable[Traversable[Link]]
  {
    override def call() : Traversable[Link] =
    {
      var links = List[Link]()

      try
      {
        val sourceInstances = caches.source.read(blockIndex, sourcePartitionIndex)
        val targetInstances = caches.target.read(blockIndex, targetPartitionIndex)

        val sourceIndexes = builtIndex(sourceInstances)
        val targetIndexes = builtIndex(targetInstances)

        for(s <- 0 until sourceInstances.size;
            tStart = if(sourceEqualsTarget && sourcePartitionIndex == targetPartitionIndex) s + 1 else 0;
            t <- tStart until targetInstances.size;
            if !indexingEnabled || compareIndexes(sourceIndexes(s), targetIndexes(t)))
        {
          val sourceInstance = sourceInstances(s)
          val targetInstance = targetInstances(t)
          val instances = SourceTargetPair(sourceInstance, targetInstance)

          if(!generateDetailedLinks)
          {
            val confidence = linkSpec.condition(instances, linkSpec.filter.threshold)

            if(confidence >= linkSpec.filter.threshold)
            {
              links ::= new Link(sourceInstance.uri, targetInstance.uri, confidence)
            }
          }
          else
          {
            for(link <- DetailedEvaluator(linkSpec.condition, instances, linkSpec.filter.threshold))
            {
              links ::= link
            }
          }
        }
      }
      catch
      {
        case ex : Exception => logger.log(Level.WARNING, "Could not execute match task", ex)
      }

      links
    }

    def builtIndex(instances : Array[Instance]) : Array[Set[Int]] =
    {
      if(indexingEnabled)
      {
        instances.map(instance => HashSet(linkSpec.condition.index(instance, linkSpec.filter.threshold).toSeq : _*))
      }
      else
      {
         Array.empty
      }
    }

    def compareIndexes(index1 : Set[Int], index2 : Set[Int]) =
    {
      index1.exists(index2.contains(_))
    }

    def evaluateCondition(instances : SourceTargetPair[Instance]) =
    {
      linkSpec.condition(instances, linkSpec.filter.threshold)
    }
  }
}
