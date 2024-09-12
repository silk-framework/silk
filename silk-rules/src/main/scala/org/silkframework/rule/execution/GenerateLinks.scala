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

import org.silkframework.cache.{EntityCache, FileEntityCache, MemoryEntityCache}
import org.silkframework.config.{Prefixes, Task}
import org.silkframework.dataset.{DataSource, LinkSink}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.rule.execution.rdb.RDBEntityIndex
import org.silkframework.rule.{LinkSpec, LinkageRule, LinkingExecutionBackend, RuntimeLinkingConfig}
import org.silkframework.runtime.activity._
import org.silkframework.util.FileUtils._
import org.silkframework.util.{CollectLogs, DPair}

import java.io.File
import java.util.UUID
import java.util.logging.{LogRecord, Logger}
import scala.util.Try

/**
 * Main task to generate links.
 */
class GenerateLinks(task: Task[LinkSpec],
                    inputs: DPair[DataSource],
                    output: Option[LinkSink],
                    runtimeConfig: RuntimeLinkingConfig = RuntimeLinkingConfig(),
                    overrideLinkageRule: Option[LinkageRule] = None)
                   (implicit prefixes: Prefixes) extends Activity[Linking] {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private val rule = overrideLinkageRule.getOrElse(task.data.rule)

  private val linkSpec = task.data.copy(rule = rule)

  /** The warnings which occurred during execution */
  @volatile private var warningLog: Seq[LogRecord] = Seq.empty

  private var children: List[ActivityControl[_]] = Nil

  private val comparisonToRestrictionConverter = new ComparisonToRestrictionConverter()

  /** The entity descriptions which define which entities are retrieved by this task */
  def entityDescs: DPair[EntitySchema] = linkSpec.entityDescriptions

  /**
   * All warnings which have been generated during executing.
   */
  def warnings: Seq[LogRecord] = warningLog

  override def initialValue: Option[Linking] = Some(Linking(task, rule))

  override def run(context: ActivityContext[Linking])
                  (implicit userContext: UserContext): Unit = {
    context.value.update(Linking(task, rule))

    warningLog = CollectLogs() {
      if(RDBEntityIndex.configured() && runtimeConfig.executionBackend == LinkingExecutionBackend.rdb && false) { //FIXME CMEM-1408: Remove false to enable RDB feature
        runRdbLinking(context)
      } else {
        runNativeLinking(context)
      }
    }
  }

  private def runRdbLinking(context: ActivityContext[Linking])
                           (implicit userContext: UserContext): Unit = {
    val entityIndex = new RDBEntityIndex(linkSpec, inputs, runtimeConfig)
    context.child(entityIndex).startBlocking()
  }

  /** Runs the native version of the linking execution */
  private def runNativeLinking(context: ActivityContext[Linking])
                       (implicit userContext: UserContext): Unit = {
    // Entity caches
    val caches = createCaches()
    // Load entities
    val loaders = for((input, cache) <- inputs zip caches) yield context.child(new CacheLoader(input, cache, runtimeConfig.sampleSizeOpt))
    children :::= loaders.toList
    if (runtimeConfig.reloadCache) {
      loaders.foreach(_.start())
    }
    if(context.status.isCanceling) return
    // Execute matching
    val sourceEqualsTarget = false // FIXME: CMEM-1975: Fix heuristic for this particular matching optimization
    val matcher = context.child(new Matcher(loaders, rule, caches, runtimeConfig, sourceEqualsTarget), 0.95)
    val updateLinks = (result: MatcherResult) => context.value.update(Linking(task, rule, result.links, LinkingStatistics(entityCount = caches.map(_.size)), result.warnings))
    matcher.value.subscribe(updateLinks)
    children ::= matcher
    matcher.startBlocking()
    updateLinks(matcher.value())

    stopLoading(context, loaders)
    cleanUpCaches(caches)

    if(context.status.isCanceling) return

    // Filter links
    val filterTask = new Filter(matcher.value().links, rule.filter)
    var filteredLinks = context.child(filterTask, 0.03).startBlockingAndGetValue()
    if(context.status.isCanceling) return

    // Include reference links
    // TODO include into Filter and execute before filtering
    if(runtimeConfig.includeReferenceLinks) {
      // Remove negative reference links and add positive reference links
      filteredLinks = (filteredLinks.toSet -- linkSpec.referenceLinks.negative ++ linkSpec.referenceLinks.positive).toSeq
    }
    runtimeConfig.linkLimit foreach { linkLimit =>
      if(filteredLinks.size > linkLimit) {
        log.info(s"Reducing ${filteredLinks.size} links to link limit of $linkLimit.")
      }
      filteredLinks = filteredLinks.take(linkLimit)
    }
    context.value.update(Linking(task, rule, filteredLinks, context.value().statistics, context.value().matcherWarnings, isDone = true))

    //Output links
    // TODO dont commit links to context if the task is not configured to hold links
    val outputTask = new OutputWriter(context.value().links, rule.linkType, rule.inverseLinkType, output)
    context.child(outputTask, 0.02).startBlocking()
    logStatistics(context)
  }

  private def logStatistics(context: ActivityContext[Linking]): Unit = {
    val result = context.value()
    log.info(s"Linking task '${task.id}' finished generating ${result.links.size} link/s having loaded " +
        s"${result.statistics.entityCount.source} source entities and ${result.statistics.entityCount.target} target entities.")
  }

  override def cancelExecution()(implicit userContext: UserContext): Unit = {
    children foreach { _.cancel()}
    super.cancelExecution()
  }

  /**
    * Makes sure that no loading activity is running after completion of this method.
    * This is relevant in cases when matching has been cancelled, but loading is still active.
    */
  private def stopLoading(context: ActivityContext[Linking], loaders: Seq[ActivityControl[_]])
                         (implicit userContext: UserContext): Unit = {
    if(loaders.exists(_.status().isRunning)) {
      context.status.updateMessage("Stopping loaders")
      for (loader <- loaders if loader.status().isRunning) {
        loader.cancel()
      }
      for (loader <- loaders if loader.status().isRunning) {
        loader.waitUntilFinished()
      }
    }
  }

  private def cleanUpCaches(caches: DPair[EntityCache]): Unit = {
    var success = false
    for (_ <- 1 to 3 if !success) {
      success = Try(caches.foreach(_.clear())).isSuccess
      if(!success) {
        Thread.sleep(50) // Another process could be deleting the cache directory. Wait a short time and try again.
      }
    }
    if (!success) {
      log.warning(s"Could not successfully clean up cache files for linking execution. Tried 3 times.")
    }
  }


  private def createCaches() = {
    val sourceIndexFunction = (entity: Entity) => runtimeConfig.executionMethod.indexEntity(entity, rule, sourceOrTarget = true)
    val targetIndexFunction = (entity: Entity) => runtimeConfig.executionMethod.indexEntity(entity, rule, sourceOrTarget = false)

    val sourceSchema = comparisonToRestrictionConverter.extendEntitySchemaWithLinkageRuleRestriction(entityDescs.source, rule, sourceOrTarget = true)
    val targetSchema = comparisonToRestrictionConverter.extendEntitySchemaWithLinkageRuleRestriction(entityDescs.target, rule, sourceOrTarget = false)
    if (runtimeConfig.useFileCache) {
      val cacheDir = new File(runtimeConfig.homeDir + "/entityCache/" + task.id + UUID.randomUUID().toString)

      DPair(
        source = new FileEntityCache(sourceSchema, sourceIndexFunction, cacheDir + "_source/", runtimeConfig),
        target = new FileEntityCache(targetSchema, targetIndexFunction, cacheDir + "_target/", runtimeConfig)
      )
    } else {
      DPair(
        source = new MemoryEntityCache(sourceSchema, sourceIndexFunction, runtimeConfig),
        target = new MemoryEntityCache(targetSchema, targetIndexFunction, runtimeConfig)
      )
    }
  }
}