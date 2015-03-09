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

import java.io.File
import java.util.logging.LogRecord
import de.fuberlin.wiwiss.silk.cache.{FileEntityCache, MemoryEntityCache}
import de.fuberlin.wiwiss.silk.config.{LinkSpecification, RuntimeConfig}
import de.fuberlin.wiwiss.silk.dataset.{Dataset,DataSink, DataSource}
import de.fuberlin.wiwiss.silk.entity.{Entity, Link}
import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, ValueHolder, Activity}
import de.fuberlin.wiwiss.silk.util.FileUtils._
import de.fuberlin.wiwiss.silk.util.{CollectLogs, DPair}

/**
 * Main task to generate links.
 */
class GenerateLinks(inputs: DPair[DataSource],
                    linkSpec: LinkSpecification,
                    outputs: Seq[DataSink] = Seq.empty,
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
        //TODO loadTask.statusLogLevel = statusLogLevel
        //TODO loadTask.progressLogLevel = progressLogLevel
        Activity.execute(loader)
      }

      //Execute matching
      val links = context.executeBlocking(matcher, 0.95, context.value.update)

      //Filter links
      val filterTask = new Filter(links, linkSpec.rule.filter)
      val filteredLinks = context.executeBlocking(filterTask, 0.03, context.value.update)

      //Output links
      val outputTask = new OutputWriter(filteredLinks, linkSpec.rule.linkType, outputs)
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

  def fromSources(inputs: Traversable[Dataset],
                  linkSpec: LinkSpecification,
                  outputs: Seq[Dataset] = Seq.empty,
                  runtimeConfig: RuntimeConfig = RuntimeConfig()) = {
    val sourcePair = DPair.fromSeq(linkSpec.datasets.map(_.datasetId).map(id => inputs.find(_.id == id).get.source))
    val sinks = outputs.map(_.sink)
    new GenerateLinks(sourcePair, linkSpec, sinks, runtimeConfig)
  }

  def empty = new GenerateLinks(DPair.empty, LinkSpecification())
}