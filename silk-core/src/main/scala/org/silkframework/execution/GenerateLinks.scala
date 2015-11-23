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
                    runtimeConfig: RuntimeConfig = RuntimeConfig()) extends Activity[Seq[Link]] {

  /** The task used for loading the entities into the cache */
  @volatile private var loader: Loader = null

  /** The task used for matching the entities */
  @volatile private var matcher: Matcher = null

  /** The warnings which occurred during execution */
  @volatile private var warningLog: Seq[LogRecord] = Seq.empty

  /** The entity descriptions which define which entities are retrieved by this task */
  def entityDescs = linkSpec.entityDescriptions

  /**
   * All warnings which have been generated during executing.
   */
  def warnings = warningLog

  override def initialValue = Some(Seq.empty)

  override def run(context: ActivityContext[Seq[Link]]) = {
    //TODO statusLogLevel = runtimeConfig.logLevel
    //TODO progressLogLevel = runtimeConfig.logLevel

    context.value.update(Seq.empty)

    warningLog = CollectLogs() {
      //Entity caches
      val caches = createCaches()

      //Create activities
      loader = new Loader(inputs, caches)
      matcher = new Matcher(linkSpec.rule, caches, runtimeConfig)

      //Load entities
      if (runtimeConfig.reloadCache) {
        val loaderControl = context.executeBackground(loader)
        // Wait until the caches are being written
        while (!loaderControl.status().isInstanceOf[Status.Finished] && !(caches.source.isWriting && caches.target.isWriting)) {
          Thread.sleep(100)
        }
      }

      //Execute matching
      context.executeBlocking(matcher, 0.95, context.value.update)

      //Filter links
      val filterTask = new Filter(context.value(), linkSpec.rule.filter)
      context.executeBlocking(filterTask, 0.03, context.value.update)

      //Output links
      val outputTask = new OutputWriter(context.value(), linkSpec.rule.linkType, outputs)
      context.executeBlocking(outputTask)
    }
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