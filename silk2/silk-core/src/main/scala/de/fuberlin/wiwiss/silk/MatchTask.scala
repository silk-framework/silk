package de.fuberlin.wiwiss.silk

import cache.EntityCache
import config.RuntimeConfig
import entity.Link
import java.util.logging.Level
import linkagerule.LinkageRule
import java.util.concurrent._
import collection.mutable.{SynchronizedBuffer, Buffer, ArrayBuffer}
import util.DPair
import scala.math.{min, max}
import util.task.ValueTask

/**
 * Executes the matching.
 * Generates links between the entities according to a linkage rule.
 *
 * @param linkageRule The linkage rules used for matching
 * @param caches The pair of caches which contains the entities to be matched
 * @param runtimeConfig The runtime configuration
 * @param sourceEqualsTarget Can be set to true if the source and the target cache are equal to enable faster matching in that case.
 */
class MatchTask(linkageRule: LinkageRule,
                caches: DPair[EntityCache],
                runtimeConfig: RuntimeConfig = RuntimeConfig(),
                sourceEqualsTarget: Boolean = false) extends ValueTask[Seq[Link]](Seq.empty) {

  /** The name of this task. */
  taskName = "MatchTask"

  /** The buffer which holds the generated links. */
  private val linkBuffer = new ArrayBuffer[Link]() with SynchronizedBuffer[Link]

  /** Indicates if this task has been canceled. */
  @volatile private var cancelled = false

  /**
   * Executes the matching.
   */
  override def execute(): Buffer[Link] = {
    require(caches.source.blockCount == caches.target.blockCount, "sourceCache.blockCount == targetCache.blockCount")

    //Reset properties
    linkBuffer.clear()
    cancelled = false

    //Create execution service for the matching tasks
    val startTime = System.currentTimeMillis()
    val executorService = Executors.newFixedThreadPool(runtimeConfig.numThreads)
    val executor = new ExecutorCompletionService[Traversable[Link]](executorService)

    //Start matching thread scheduler
    val scheduler = new SchedulerThread(executor)
    scheduler.start()

    //Process finished tasks
    var finishedTasks = 0
    while (!cancelled && (scheduler.isAlive || finishedTasks < scheduler.taskCount)) {
      val result = executor.poll(100, TimeUnit.MILLISECONDS)
      if (result != null) {
        linkBuffer.appendAll(result.get)
        value.update(linkBuffer)
        finishedTasks += 1

        //Update status
        val statusPrefix = if (scheduler.isAlive) "Matching (loading):" else "Matching:"
        val statusTasks = " " + finishedTasks + " tasks "
        val statusLinks = " " + linkBuffer.size + " links."
        updateStatus(statusPrefix + statusTasks + statusLinks, finishedTasks.toDouble / scheduler.taskCount)
      }
    }

    //Shutdown
    if (scheduler.isAlive)
      scheduler.interrupt()

    if(cancelled)
      executorService.shutdownNow()
    else
      executorService.shutdown()

    //Log result
    val time = ((System.currentTimeMillis - startTime) / 1000.0) + " seconds"
    if (cancelled)
      logger.info("Matching cancelled after " + time)
    else
      logger.info("Executed matching in " + time)

    linkBuffer
  }

  override def stopExecution() {
    cancelled = true
  }

  def clear() {
    linkBuffer.clear()
  }

  /**
   * Monitors the entity caches and schedules new matching threads whenever a new partition has been loaded.
   */
  private class SchedulerThread(executor: CompletionService[Traversable[Link]]) extends Thread {
    @volatile var taskCount = 0

    private var sourcePartitions = new Array[Int](caches.source.blockCount)
    private var targetPartitions = new Array[Int](caches.target.blockCount)

    override def run() {
      try {
        while (true) {
          val sourceLoaded = !caches.source.isWriting
          val targetLoaded = !caches.target.isWriting

          updateSourcePartitions(sourceLoaded)
          updateTargetPartitions(targetLoaded)

          if (sourceLoaded && targetLoaded) {
            return
          }

          Thread.sleep(1000)
        }
      } catch {
        case ex: InterruptedException =>
      }
    }

    private def updateSourcePartitions(includeLastPartitions: Boolean) {
      val newSourcePartitions = {
        for (block <- 0 until caches.source.blockCount) yield {
          if (includeLastPartitions)
            caches.source.partitionCount(block)
          else
            max(0, caches.source.partitionCount(block) - 1)
        }
      }.toArray

      for (block <- 0 until caches.source.blockCount;
           sourcePartition <- sourcePartitions(block) until newSourcePartitions(block);
           targetStart = if (!sourceEqualsTarget) 0 else sourcePartition;
           targetPartition <- targetStart until targetPartitions(block)) {
        newMatcher(block, sourcePartition, targetPartition)
      }

      sourcePartitions = newSourcePartitions
    }

    private def updateTargetPartitions(includeLastPartitions: Boolean) {
      val newTargetPartitions = {
        for (block <- 0 until caches.target.blockCount) yield {
          if (includeLastPartitions) {
            caches.target.partitionCount(block)
          } else {
            max(0, caches.target.partitionCount(block) - 1)
          }
        }
      }.toArray

      for (block <- 0 until caches.target.blockCount;
           targetPartition <- targetPartitions(block) until newTargetPartitions(block);
           sourceEnd = if (!sourceEqualsTarget) sourcePartitions(block) else min(sourcePartitions(block), targetPartition + 1);
           sourcePartition <- 0 until sourceEnd) {
        newMatcher(block, sourcePartition, targetPartition)
      }

      targetPartitions = newTargetPartitions
    }

    private def newMatcher(block: Int, sourcePartition: Int, targetPartition: Int) {
      executor.submit(new Matcher(block, sourcePartition, targetPartition))
      taskCount += 1
    }
  }

  /**
   * Matches the entities of two partitions.
   */
  private class Matcher(blockIndex: Int, sourcePartitionIndex: Int, targetPartitionIndex: Int) extends Callable[Traversable[Link]] {
    override def call(): Traversable[Link] = {
      var links = List[Link]()

      try {
        val sourcePartition = caches.source.read(blockIndex, sourcePartitionIndex)
        val targetPartition = caches.target.read(blockIndex, targetPartitionIndex)

        //Iterate over all entities in the source partition
        var s = 0
        while(s < sourcePartition.size) {
          //Iterate over all entities in the target partition
          var t = if (sourceEqualsTarget && sourcePartitionIndex == targetPartitionIndex) s + 1 else 0
          while(t < targetPartition.size) {
            //Check if the indices match
            if(!runtimeConfig.blocking.isEnabled || (sourcePartition.indices(s) matches targetPartition.indices(t))) {
              val sourceEntity = sourcePartition.entities(s)
              val targetEntity = targetPartition.entities(t)
              val entities = DPair(sourceEntity, targetEntity)
              val attachedEntities = if(runtimeConfig.generateLinksWithEntities) Some(entities) else None
              val confidence = linkageRule(entities, 0.0)

              if (confidence >= 0.0) {
                links ::= new Link(sourceEntity.uri, targetEntity.uri, Some(confidence), attachedEntities)
              }
            }
            t += 1
          }
          s += 1
        }

      }
      catch {
        case ex: Exception => logger.log(Level.WARNING, "Could not execute match task", ex)
      }

      links
    }
  }
}
