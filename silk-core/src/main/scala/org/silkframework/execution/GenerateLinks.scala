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
import org.silkframework.dataset.{LinkSink, DataSink, DataSource, Dataset}
import org.silkframework.entity.{Entity, Link}
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
      loader = new Loader(inputs, caches)
      matcher = new Matcher(linkSpec.rule, caches, runtimeConfig)

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
      val filteredLinks = context.child(filterTask, 0.03).startBlockingAndGetValue()
      context.value.update(Linking(filteredLinks, LinkingStatistics(entityCount = caches.map(_.size))))
      if(canceled) return

      //Output links
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
    val indexFunction = (entity: Entity) => runtimeConfig.executionMethod.indexEntity(entity, linkSpec.rule)

    if (runtimeConfig.useFileCache) {
      val cacheDir = new File(runtimeConfig.homeDir + "/entityCache/" + linkSpec.id)

      DPair(
        source = new FileEntityCache(entityDescs.source, indexFunction, cacheDir + "/source/", runtimeConfig),
        target = new FileEntityCache(entityDescs.target, indexFunction, cacheDir + "/target/", runtimeConfig)
      )
    } else {
      DPair(
        source = new MemoryEntityCache(entityDescs.source, indexFunction, runtimeConfig),
        target = new MemoryEntityCache(entityDescs.target, indexFunction, runtimeConfig)
      )
    }
  }
}

object GenerateLinks {

  def fromSources(datasets: Traversable[Dataset],
                  linkSpec: LinkSpecification,
                  runtimeConfig: RuntimeConfig = RuntimeConfig()) = {
    val sourcePair = linkSpec.findSources(datasets)
    val outputs = linkSpec.outputs.flatMap(o => datasets.find(_.id == o)).map(_.sink)
    new GenerateLinks(sourcePair, linkSpec, outputs, runtimeConfig)
  }

  def empty = new GenerateLinks(DPair.empty, LinkSpecification(), Seq.empty)
}