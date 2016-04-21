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

package org.silkframework.execution

import java.io.File
import java.util.logging.LogRecord

import org.silkframework.cache.{FileEntityCache, MemoryEntityCache}
import org.silkframework.config.{LinkSpecification, RuntimeConfig}
import org.silkframework.dataset.{DataSource, Dataset, LinkSink}
import org.silkframework.entity.Entity
import org.silkframework.runtime.activity.{Activity, ActivityContext, Status}
import org.silkframework.util.FileUtils._
import org.silkframework.util.{CollectLogs, DPair}

/**
 * Main task to generate links.
 */
class GenerateLinks(inputs: DPair[DataSource],
                    linkSpec: LinkSpecification,
                    outputs: Seq[LinkSink],
                    runtimeConfig: RuntimeConfig = RuntimeConfig()) extends Activity[Linking] {

  /** The task used for loading the entities into the cache */
  @volatile private var loader: Loader = null

  /** The task used for matching the entities */
  @volatile private var matcher: Matcher = null

  /** The warnings which occurred during execution */
  @volatile private var warningLog: Seq[LogRecord] = Seq.empty

  /** Indicates if this task has been canceled. */
  @volatile private var canceled = false

  /** The entity descriptions which define which entities are retrieved by this task */
  def entityDescs = linkSpec.entityDescriptions

  /**
   * All warnings which have been generated during executing.
   */
  def warnings = warningLog

  override def initialValue = Some(Linking())

  override def run(context: ActivityContext[Linking]): Unit = {
    context.value.update(Linking())

    warningLog = CollectLogs() {
      //Entity caches
      val caches = createCaches()

      //Create activities
      loader = new Loader(inputs, caches, runtimeConfig.sampleSizeOpt)
      val sourceEqualsTarget = linkSpec.dataSelections.source == linkSpec.dataSelections.target
      matcher = new Matcher(linkSpec.rule, caches, runtimeConfig, sourceEqualsTarget)

      //Load entities
      if (runtimeConfig.reloadCache) {
        val loaderControl = context.child(loader, 0.0)
        loaderControl.start()
        // Wait until the caches are being written
        while (!loaderControl.status().isInstanceOf[Status.Finished] && !(caches.source.isWriting && caches.target.isWriting)) {
          Thread.sleep(100)
          if(canceled) return
        }
      }

      //Execute matching
      val matcherContext = context.child(matcher, 0.95)
      matcherContext.value.onUpdate(links => context.value.update(Linking(links, LinkingStatistics(entityCount = caches.map(_.size)))))
      matcherContext.startBlocking()
      if(canceled) return

      //Filter links
      val filterTask = new Filter(matcherContext.value(), linkSpec.rule.filter)
      var filteredLinks = context.child(filterTask, 0.03).startBlockingAndGetValue()
      if(canceled) return

      // Include reference links
      // TODO include into Filter and execute before filtering
      if(runtimeConfig.includeReferenceLinks) {
        // Remove negative reference links and add positive reference links
        filteredLinks = (filteredLinks.toSet -- linkSpec.referenceLinks.negative ++ linkSpec.referenceLinks.positive).toSeq
      }

      context.value.update(Linking(filteredLinks, LinkingStatistics(entityCount = caches.map(_.size))))

      //Output links
      // TODO dont commit links to context if the task is not configured to hold links
      val outputTask = new OutputWriter(context.value().links, linkSpec.rule.linkType, outputs)
      context.child(outputTask, 0.02).startBlocking()
    }
  }

  override def cancelExecution(): Unit = {
    canceled = true
    if(loader != null)
      loader.cancelExecution()
    if(matcher != null)
      matcher.cancelExecution()
  }

  private def createCaches() = {
    val sourceIndexFunction = (entity: Entity) => runtimeConfig.executionMethod.indexEntity(entity, linkSpec.rule, sourceOrTarget = true)
    val targetIndexFunction = (entity: Entity) => runtimeConfig.executionMethod.indexEntity(entity, linkSpec.rule, sourceOrTarget = false)

    if (runtimeConfig.useFileCache) {
      val cacheDir = new File(runtimeConfig.homeDir + "/entityCache/" + linkSpec.id)

      DPair(
        source = new FileEntityCache(entityDescs.source, sourceIndexFunction, cacheDir + "/source/", runtimeConfig),
        target = new FileEntityCache(entityDescs.target, targetIndexFunction, cacheDir + "/target/", runtimeConfig)
      )
    } else {
      DPair(
        source = new MemoryEntityCache(entityDescs.source, sourceIndexFunction, runtimeConfig),
        target = new MemoryEntityCache(entityDescs.target, targetIndexFunction, runtimeConfig)
      )
    }
  }
}

object GenerateLinks {

  def fromSources(datasets: Traversable[Dataset],
                  linkSpec: LinkSpecification,
                  runtimeConfig: RuntimeConfig = RuntimeConfig()) = {
    val sourcePair = linkSpec.findSources(datasets)
    val outputs = linkSpec.outputs.flatMap(o => datasets.find(_.id == o)).map(_.linkSink)
    new GenerateLinks(sourcePair, linkSpec, outputs, runtimeConfig)
  }

  def empty = new GenerateLinks(DPair.empty, LinkSpecification(), Seq.empty)
}