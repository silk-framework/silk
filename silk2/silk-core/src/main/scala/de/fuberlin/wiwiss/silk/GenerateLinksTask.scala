package de.fuberlin.wiwiss.silk

import entity.{Link, FileEntityCache, MemoryEntityCache, EntityDescription}
import java.util.logging.LogRecord
import de.fuberlin.wiwiss.silk.util.{CollectLogs, DPair}
import de.fuberlin.wiwiss.silk.output.{Output}
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import java.io.File
import de.fuberlin.wiwiss.silk.util.FileUtils._

/**
 * Main task to generate links.
 */
class GenerateLinksTask(sources: Traversable[Source],
                        linkSpec: LinkSpecification,
                        outputs: Traversable[Output] = Traversable.empty,
                        runtimeConfig: RuntimeConfig = RuntimeConfig()) extends ValueTask[Seq[Link]](Seq.empty) {

  /** The task used for loading the entities into the cache */
  @volatile private var loadTask: LoadTask = null

  /** The task used for matching the entities */
  @volatile private var matchTask: MatchTask = null

  /** The warnings which occurred during execution */
  @volatile private var warningLog: Seq[LogRecord] = Seq.empty

  /** The entity descriptions which define which entities are retrieved by this task */
  def entityDescs = linkSpec.entityDescriptions

  /** The links which have been generated so far by this task */
  def links = value.get

  /**
   * All warnings which have been generated during executing.
   */
  def warnings = warningLog

  /**
   * Stops the tasks and removes all generated links.
   */
  def clear() {
    cancel()
    value.update(Seq.empty)
  }

  override protected def execute() = {
    value.update(Seq.empty)

    warningLog = CollectLogs() {
      //Retrieve sources
      val sourcePair = linkSpec.datasets.map(_.sourceId).map(id => sources.find(_.id == id).get)

      //Entity caches
      val caches = createCaches()

      //Create tasks
      loadTask = new LoadTask(sourcePair, caches)
      matchTask = new MatchTask(linkSpec.rule, caches, runtimeConfig)

      //Load entities
      if (runtimeConfig.reloadCache) loadTask.runInBackground()

      //Execute matching
      val links = executeSubValueTask(matchTask, 0.95)

      //Filter links
      val filterTask = new FilterTask(links, linkSpec.filter)
      value.update(executeSubTask(filterTask))

      //Output links
      val outputTask = new OutputTask(value.get, linkSpec.linkType, outputs)
      executeSubTask(outputTask)
    }

    //Return generated links
    value.get
  }

  private def createCaches() = {
    if (runtimeConfig.useFileCache) {
      val cacheDir = new File(runtimeConfig.homeDir + "/entityCache/" + linkSpec.id)

      DPair(
        source = new FileEntityCache(entityDescs.source, linkSpec.rule.index(_), cacheDir + "/source/", runtimeConfig),
        target = new FileEntityCache(entityDescs.target, linkSpec.rule.index(_), cacheDir + "/target/", runtimeConfig)
      )
    } else {
      DPair(
        source = new MemoryEntityCache(entityDescs.source, linkSpec.rule.index(_), runtimeConfig),
        target = new MemoryEntityCache(entityDescs.target, linkSpec.rule.index(_), runtimeConfig)
      )
    }
  }

  override def stopExecution() {
    if (loadTask != null) loadTask.cancel()
    if (matchTask != null) matchTask.cancel()
  }
}

object GenerateLinksTask {
  def empty = new GenerateLinksTask(Traversable.empty, LinkSpecification())
}