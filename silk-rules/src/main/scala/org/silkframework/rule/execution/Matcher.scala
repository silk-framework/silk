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

package org.silkframework.rule.execution

import org.silkframework.cache.EntityCache
import org.silkframework.entity.Link
import org.silkframework.rule.{LinkageRule, RuntimeLinkingConfig}
import org.silkframework.runtime.activity.Status.Waiting
import org.silkframework.runtime.activity.{Activity, ActivityContext, ActivityControl, UserContext}
import org.silkframework.runtime.execution.Execution
import org.silkframework.util.DPair

import java.util.concurrent._
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import java.util.logging.{Level, Logger}
import scala.math.min

/**
 * Executes the matching.
 * Generates links between the entities according to a linkage rule.
 *
 * @param linkageRule The linkage rules used for matching
 * @param caches The pair of caches which contains the entities to be matched
 * @param runtimeConfig The runtime configuration
 * @param sourceEqualsTarget Can be set to true if the source and the target cache are equal to enable faster matching in that case.
 */
class Matcher(loaders: DPair[ActivityControl[Unit]],
              linkageRule: LinkageRule,
              caches: DPair[EntityCache],
              runtimeConfig: RuntimeLinkingConfig = RuntimeLinkingConfig(),
              sourceEqualsTarget: Boolean = false) extends Activity[MatcherResult] {

  /** The name of this task. */
  override def name: String = "MatchTask"
  final val POLL_TIMEOUT_MS = 1000
  final val minLogDelayInMs = 5000
  final val MAX_PARTITION_MATCHER_QUEUE_SIZE = 1000

  private val log = Logger.getLogger(getClass.getName)

  /**
   * Executes the matching.
   */
  override def run(context: ActivityContext[MatcherResult])
                  (implicit userContext: UserContext): Unit = {
    val startTime = System.currentTimeMillis()
    def timeoutReached: Boolean = System.currentTimeMillis() - startTime > runtimeConfig.executionTimeout.getOrElse(Long.MaxValue)
    init(context)
    //Create execution service for the matching tasks
    val executorService = boundedExecutionService()
    val executor = new ExecutorCompletionService[IndexedSeq[Link]](executorService)

    //Start matching thread scheduler
    val errors = new AtomicReference[Seq[Throwable]](Seq.empty)
    val scheduler = new SchedulerThread(executor, errors)
    scheduler.start()

    //Process finished tasks
    val finishedTasks = new AtomicInteger()
    val logProgress = progressLogger(context, finishedTasks, scheduler)
    while (!cancelled && (scheduler.isAlive || finishedTasks.get() < scheduler.taskCount) && errors.get().isEmpty) {
      for(result <- poll(executor, context)) {
        context.value.updateWith(_.addLinks(result))
        finishedTasks.incrementAndGet()
        if(runtimeConfig.linkLimit.getOrElse(Int.MaxValue) <= context.value().links.size) {
          context.value.updateWith(_.addWarning(s"The configured maximum number of links has been reached and the matching has been stopped."))
          cancelled = true
        } else if(timeoutReached) {
          context.value.updateWith(_.addWarning(s"The configured timeout has been exceeded and the matching has been stopped."))
          cancelled = true
        } else if(Thread.currentThread().isInterrupted || context.status.isCanceling) {
          cancelled = true
        }
        logProgress()
      }
    }

    if (errors.get().nonEmpty) {
      handleErrors(errors.get())
    }

    for (result <- poll(executor, context)) {
      context.value.updateWith(_.addLinks(result))
      updateStatus(context, finishedTasks.get(), scheduler.taskCount)
    }

    shutdown(executorService, scheduler)
  }

  private def poll[T](executor: ExecutorCompletionService[T], context: ActivityContext[MatcherResult]): Option[T] = {
    try {
      executor.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS) match {
        case null =>
          // No result available: make sure that loaders are not blocked
          for (runningLoader <- loaders.filter(_.status().isInstanceOf[Waiting])) {
            // join blocked loader
            log.info("No more threads available. Fully executing loader first.")
            runningLoader.waitUntilFinished()
          }
          None
        case future: Future[T] =>
          Some(future.get())
      }
    } catch {
      case _: InterruptedException =>
        cancelled = true
        None
    }
  }

  private def shutdown(executorService: ExecutorService, scheduler: SchedulerThread): Unit = {
    if (scheduler.isAlive) {
      scheduler.interrupt()
    }

    if (cancelled) {
      executorService.shutdownNow()
    } else {
      executorService.shutdown()
    }
  }

  private def boundedExecutionService(): ExecutorService = {
    Execution.createFixedThreadPool(
      "Matcher",
      runtimeConfig.numThreads,
      new LinkedBlockingQueue[Runnable](MAX_PARTITION_MATCHER_QUEUE_SIZE), // bounded queue to keep memory foot print constant
      Some(new ThreadPoolExecutor.CallerRunsPolicy())
    )
  }

  private def init(context: ActivityContext[MatcherResult]): Unit = {
    require(caches.source.blockCount == caches.target.blockCount, "sourceCache.blockCount == targetCache.blockCount")

    //Reset properties
    context.value.update(MatcherResult())
    cancelled = false
  }

  private def progressLogger(context: ActivityContext[MatcherResult],
                             finishedTasks: AtomicInteger,
                             scheduler: SchedulerThread): () => Unit = {
    var lastLog: Long = 0
    () => {
      if(System.currentTimeMillis() - lastLog > minLogDelayInMs) {
        //Update status
        updateStatus(context, finishedTasks.get(), scheduler.taskCount)
        lastLog = System.currentTimeMillis()
      }
    }
  }

  private def handleErrors(errors: Seq[Throwable]) = {
    errors match {
      case Seq(error) =>
        throw error
      case _ =>
        // There are multiple errors. We log all and throw the first one
        errors.foreach(log.log(Level.WARNING, "Error during matching", _))
        throw errors.head
    }
  }

  private def updateStatus(context: ActivityContext[MatcherResult], finishedTasks: Int, nrOfTasks: Int): Unit = {
    val statusPrefix = if (loaders.exists(_.status().isRunning)) "Matching (loading):" else "Matching:"
    val statusTasks = " " + finishedTasks + " tasks "
    val statusLinks = " " + context.value().links.size + " links."
    context.status.update(statusPrefix + statusTasks + statusLinks, finishedTasks.toDouble / nrOfTasks)
  }

  /**
   * Monitors the entity caches and schedules new matching threads whenever a new partition has been loaded.
   */
  private class SchedulerThread(executor: CompletionService[IndexedSeq[Link]], errors: AtomicReference[Seq[Throwable]]) extends Thread {
    @volatile var taskCount = 0

    private var sourcePartitions = new Array[Int](caches.source.blockCount)
    private var targetPartitions = new Array[Int](caches.target.blockCount)

    override def run(): Unit = {
      try {
        var sourceLoading = true
        var targetLoading = true
        var loaderSuccessful = true

        while {
          sourceLoading = loaders.source.status().isRunning
          targetLoading = loaders.target.status().isRunning
          loaderSuccessful = !Seq(loaders.source, loaders.target).exists(_.status.get.exists(_.failed))

          updateSourcePartitions()
          updateTargetPartitions()

          Thread.sleep(500)

          (sourceLoading || targetLoading) && loaderSuccessful
        } do()
        val failedLoaders = Seq(loaders.source, loaders.target).filter(_.status.get.exists(_.failed))
        if(failedLoaders.nonEmpty) { // One of the loaders failed
          val loaderErrorMessages =
            for(loader <- failedLoaders) yield {
              val status = loader.status.get.get
              new RuntimeException(s"${loader.name} task failed: ${status.message}", status.exception.orNull)
            }
          errors.set(loaderErrorMessages)
        }
      } catch {
        case _: InterruptedException =>
      }
    }

    private def updateSourcePartitions(): Unit = {
      val newSourcePartitions = {
        for (block <- 0 until caches.source.blockCount) yield {
          caches.source.partitionCount(block)
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

    private def updateTargetPartitions(): Unit = {
      val newTargetPartitions = {
        for (block <- 0 until caches.target.blockCount) yield {
          caches.target.partitionCount(block)
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

    private def newMatcher(block: Int, sourcePartition: Int, targetPartition: Int): Unit = {
      executor.submit(new PartitionMatcher(block, sourcePartition, targetPartition))
      taskCount += 1
    }
  }

  /**
   * Matches the entities of two partitions.
   */
  private class PartitionMatcher(blockIndex: Int, sourcePartitionIndex: Int, targetPartitionIndex: Int) extends Callable[IndexedSeq[Link]] {
    override def call(): IndexedSeq[Link] = {
      var links = IndexedSeq[Link]()

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
            if(!linkageRule.excludeSelfReferences || entityPair.source.uri != entityPair.target.uri) {
              val confidence = linkageRule(entityPair, 0.0)
              if (confidence >= 0.0) {
                links = links :+ Link(entityPair.source.uri, entityPair.target.uri, Some(confidence), attachedEntities)
              }
            }
          } else {
            links = links :+ Link(entityPair.source.uri, entityPair.target.uri, None, attachedEntities)
          }
        }
      }
      catch {
        case ex: Exception =>  if(!cancelled) {
          log.log(Level.WARNING, s"Could not execute match task for block $blockIndex, source partition $sourcePartitionIndex, target partition $targetPartitionIndex", ex)
        }
      }

      links
    }
  }
}

/**
  * The (possibly intermediate) result of a matcher.
  *
  * @param links The generated links
  * @param warnings Issues that occurred during matching
  */
case class MatcherResult(links: IndexedSeq[Link] = IndexedSeq.empty, warnings: Seq[String] = Seq.empty) {

  /**
    * Appends links and returns the updated matcher result.
    */
  def addLinks(newLinks: IndexedSeq[Link]): MatcherResult = {
    copy(links = links ++ newLinks)
  }

  /**
    * Appends a warning and returns the updated matcher result.
    */
  def addWarning(warning: String): MatcherResult = {
    copy(warnings = warnings :+ warning)
  }

}

