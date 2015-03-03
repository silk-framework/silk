/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.execution

import java.util.concurrent._
import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.cache.EntityCache
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.runtime.task.{ValueHolder, TaskContext, Task}
import de.fuberlin.wiwiss.silk.util.DPair
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, Buffer}
import scala.math.{max, min}

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
                sourceEqualsTarget: Boolean = false) extends Task {

  /** The name of this task. */
  override def taskName = "MatchTask"

  private val log = Logger.getLogger(getClass.getName)

  /** Indicates if this task has been canceled. */
  @volatile private var cancelled = false

  /** Holds the links generated so far. */
  val links = new ValueHolder[IndexedSeq[Link]](IndexedSeq.empty)

  /**
   * Executes the matching.
   */
  override def execute(context: TaskContext) = {
    require(caches.source.blockCount == caches.target.blockCount, "sourceCache.blockCount == targetCache.blockCount")

    //Reset properties
    links.update(IndexedSeq.empty)
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
        links.update(links() ++ result.get)
        finishedTasks += 1

        //Update status
        val statusPrefix = if (scheduler.isAlive) "Matching (loading):" else "Matching:"
        val statusTasks = " " + finishedTasks + " tasks "
        val statusLinks = " " + links().size + " links."
        context.updateStatus(statusPrefix + statusTasks + statusLinks, finishedTasks.toDouble / scheduler.taskCount)
      }
    }

    //Shutdown
    if (scheduler.isAlive)
      scheduler.interrupt()

    if(cancelled)
      executorService.shutdownNow()
    else
      executorService.shutdown()

//TODO
//    Log result
//    val time = ((System.currentTimeMillis - startTime) / 1000.0) + " seconds"
//    if (cancelled)
//      log.log(statusLogLevel, "Matching cancelled after " + time)
//    else
//      log.log(statusLogLevel, "Executed matching in " + time)
  }

  override def cancelExecution() {
    cancelled = true
  }

  def clear() {
    links.update(IndexedSeq.empty)
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
        //Read source and target partitions
        val sourcePartition = caches.source.read(blockIndex, sourcePartitionIndex)
        val targetPartition = caches.target.read(blockIndex, targetPartitionIndex)

        //Determine if a full comparison should be done
        val full = !(sourceEqualsTarget && sourcePartitionIndex == targetPartitionIndex)

        //Retrieve all comparison pairs
        val entityPairs = runtimeConfig.executionMethod.comparisonPairs(sourcePartition, targetPartition, full)

        //Compare each pair of entities
        for(entityPair <- entityPairs) {
          val attachedEntities = if(runtimeConfig.generateLinksWithEntities) Some(entityPair) else None

          if(!runtimeConfig.indexingOnly) {
            val confidence = linkageRule(entityPair, 0.0)
            if (confidence >= 0.0) {
              links ::= new Link(entityPair.source.uri, entityPair.target.uri, Some(confidence), attachedEntities)
            }
          } else {
            links ::= new Link(entityPair.source.uri, entityPair.target.uri, None, attachedEntities)
          }
        }
      }
      catch {
        case ex: Exception => log.log(Level.WARNING, "Could not execute match task", ex)
      }

      links
    }
  }
}
