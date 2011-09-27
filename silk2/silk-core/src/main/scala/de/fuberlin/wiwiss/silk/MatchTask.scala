package de.fuberlin.wiwiss.silk

import config.RuntimeConfig
import entity.EntityCache
import linkagerule.evaluation.DetailedEvaluator
import linkagerule.LinkSpecification
import java.util.logging.Level
import output.Link
import java.util.concurrent._
import collection.mutable.{SynchronizedBuffer, Buffer, ArrayBuffer}
import util.DPair
import scala.math.{min, max}
import util.task.ValueTask

/**
 * Executes the matching.
 * Generates links between the entitys according to the link specification.
 */
class MatchTask(linkSpec: LinkSpecification,
                caches: DPair[EntityCache],
                runtimeConfig: RuntimeConfig = RuntimeConfig(),
                sourceEqualsTarget: Boolean = false) extends ValueTask[Seq[Link]](Seq.empty) {
  taskName = "Matching"

  private val linkBuffer = new ArrayBuffer[Link]() with SynchronizedBuffer[Link]

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
    if (scheduler.isAlive) {
      scheduler.interrupt()
    }
    if (cancelled) {
      executorService.shutdownNow()
    }
    else {
      executorService.shutdown()
    }

    //Log result
    val time = ((System.currentTimeMillis - startTime) / 1000.0) + " seconds"
    if (cancelled) {
      logger.info("Matching cancelled after " + time)
    }
    else {
      logger.info("Executed matching in " + time)
    }

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
          if (includeLastPartitions) {
            caches.source.partitionCount(block)
          } else {
            max(0, caches.source.partitionCount(block) - 1)
          }
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
   * Matches the entitys of two partitions.
   */
  private class Matcher(blockIndex: Int, sourcePartitionIndex: Int, targetPartitionIndex: Int) extends Callable[Traversable[Link]] {
    override def call(): Traversable[Link] = {
      var links = List[Link]()

      try {
        val sourcePartition = caches.source.read(blockIndex, sourcePartitionIndex)
        val targetPartition = caches.target.read(blockIndex, targetPartitionIndex)

        for (s <- 0 until sourcePartition.size;
             tStart = if (sourceEqualsTarget && sourcePartitionIndex == targetPartitionIndex) s + 1 else 0;
             t <- tStart until targetPartition.size;
             if !runtimeConfig.blocking.isEnabled || (sourcePartition.indices(s) matches targetPartition.indices(t))) {
          val sourceEntity = sourcePartition.entities(s)
          val targetEntity = targetPartition.entities(t)
          val entities = DPair(sourceEntity, targetEntity)

          if (!runtimeConfig.generateDetailedLinks) {
            val confidence = linkSpec.rule(entities, 0.0)

            if (confidence >= 0.0) {
              links ::= new Link(sourceEntity.uri, targetEntity.uri, confidence)
            }
          } else {
            for (link <- DetailedEvaluator(linkSpec.rule, entities, 0.0)) {
              links ::= link
            }
          }
        }
      }
      catch {
        case ex: Exception => logger.log(Level.WARNING, "Could not execute match task", ex)
      }

      links
    }
  }
}
