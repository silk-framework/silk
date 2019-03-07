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

import java.io.File
import java.util.UUID
import java.util.logging.{LogRecord, Logger}

import org.silkframework.cache.{EntityCache, FileEntityCache, MemoryEntityCache}
import org.silkframework.dataset.{DataSource, LinkSink}
import org.silkframework.entity.{Entity, EntitySchema, Link}
import org.silkframework.rule.{LinkSpec, RuntimeLinkingConfig}
import org.silkframework.runtime.activity._
import org.silkframework.util.FileUtils._
import org.silkframework.util.{CollectLogs, DPair, Identifier}

import scala.util.Try

/**
 * Main task to generate links.
 */
class GenerateLinks(id: Identifier,
                    inputs: DPair[DataSource],
                    linkSpec: LinkSpec,
                    outputs: Seq[LinkSink],
                    runtimeConfig: RuntimeLinkingConfig = RuntimeLinkingConfig()) extends Activity[Linking] {
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  /** The warnings which occurred during execution */
  @volatile private var warningLog: Seq[LogRecord] = Seq.empty

  private var children: List[ActivityControl[_]] = Nil

  /** The entity descriptions which define which entities are retrieved by this task */
  def entityDescs: DPair[EntitySchema] = linkSpec.entityDescriptions

  /**
   * All warnings which have been generated during executing.
   */
  def warnings: Seq[LogRecord] = warningLog

  override def initialValue = Some(Linking(rule = linkSpec.rule))

  override def run(context: ActivityContext[Linking])
                  (implicit userContext: UserContext): Unit = {
    context.value.update(Linking(rule = linkSpec.rule))

    warningLog = CollectLogs() {
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
      val sourceEqualsTarget = linkSpec.dataSelections.source == linkSpec.dataSelections.target
      val matcher = context.child(new Matcher(loaders, linkSpec.rule, caches, runtimeConfig, sourceEqualsTarget), 0.95)
      val updateLinks = (links: Seq[Link]) => context.value.update(Linking(linkSpec.rule, links, LinkingStatistics(entityCount = caches.map(_.size))))
      matcher.value.subscribe(updateLinks)
      children ::= matcher
      matcher.startBlocking()

      cleanUpCaches(caches)
      if(context.status.isCanceling) return

      // Filter links
      val filterTask = new Filter(matcher.value(), linkSpec.rule.filter)
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

      context.value.update(Linking(linkSpec.rule, filteredLinks, LinkingStatistics(entityCount = caches.map(_.size))))

      //Output links
      // TODO dont commit links to context if the task is not configured to hold links
      val outputTask = new OutputWriter(context.value().links, linkSpec.rule.linkType, outputs)
      context.child(outputTask, 0.02).startBlocking()
    }
  }

  override def cancelExecution()(implicit userContext: UserContext): Unit = {
    children foreach { _.cancel()}
    super.cancelExecution()
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
    val sourceIndexFunction = (entity: Entity) => runtimeConfig.executionMethod.indexEntity(entity, linkSpec.rule, sourceOrTarget = true)
    val targetIndexFunction = (entity: Entity) => runtimeConfig.executionMethod.indexEntity(entity, linkSpec.rule, sourceOrTarget = false)

    if (runtimeConfig.useFileCache) {
      val cacheDir = new File(runtimeConfig.homeDir + "/entityCache/" + id + UUID.randomUUID().toString)

      DPair(
        source = new FileEntityCache(entityDescs.source, sourceIndexFunction, cacheDir + "_source/", runtimeConfig),
        target = new FileEntityCache(entityDescs.target, targetIndexFunction, cacheDir + "_target/", runtimeConfig)
      )
    } else {
      DPair(
        source = new MemoryEntityCache(entityDescs.source, sourceIndexFunction, runtimeConfig),
        target = new MemoryEntityCache(entityDescs.target, targetIndexFunction, runtimeConfig)
      )
    }
  }
}