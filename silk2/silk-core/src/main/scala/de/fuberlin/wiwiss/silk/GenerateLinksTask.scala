package de.fuberlin.wiwiss.silk

import java.util.logging.LogRecord
import de.fuberlin.wiwiss.silk.util.{CollectLogs, SourceTargetPair}
import de.fuberlin.wiwiss.silk.output.{Output, Link}
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import java.io.File
import de.fuberlin.wiwiss.silk.instance.{FileInstanceCache, MemoryInstanceCache, InstanceSpecification}
import de.fuberlin.wiwiss.silk.util.FileUtils._

/**
 * Main task to generate links.
 */
class GenerateLinksTask(sources: Traversable[Source],
                        linkSpec: LinkSpecification,
                        outputs: Traversable[Output] = Traversable.empty,
                        runtimeConfig: RuntimeConfig = RuntimeConfig()) extends ValueTask[Seq[Link]](Seq.empty) {

  @volatile private var loadTask: LoadTask = null

  @volatile private var matchTask: MatchTask = null

  @volatile private var warningLog: Seq[LogRecord] = Seq.empty

  def links = value.get

  /**
   * All warnings which have been generated during executing.
   */
  def warnings = warningLog

  def clear() {
    cancel()
    value.update(Seq.empty)
  }

  override protected def execute() = {
    value.update(Seq.empty)

    warningLog = CollectLogs() {
      //Retrieve sources
      val sourcePair = linkSpec.datasets.map(_.sourceId).map(id => sources.find(_.id == id).get)

      //Instance caches
      val caches = createCaches()

      //Create tasks
      loadTask = new LoadTask(sourcePair, caches, linkSpec.rule.index(_))
      matchTask = new MatchTask(linkSpec, caches, runtimeConfig)

      //Load instances
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
    val instanceSpecs = InstanceSpecification.retrieve(linkSpec)

    if (runtimeConfig.useFileCache) {
      val cacheDir = new File(runtimeConfig.homeDir + "/instanceCache/" + linkSpec.id)

      SourceTargetPair(
        source = new FileInstanceCache(instanceSpecs.source, cacheDir + "/source/", runtimeConfig),
        target = new FileInstanceCache(instanceSpecs.target, cacheDir + "/target/", runtimeConfig)
      )
    } else {
      SourceTargetPair(
        source = new MemoryInstanceCache(instanceSpecs.source, runtimeConfig),
        target = new MemoryInstanceCache(instanceSpecs.target, runtimeConfig)
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