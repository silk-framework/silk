package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.instance.{MemoryInstanceCache, Instance, InstanceSpecification}
import collection.mutable.Buffer
import java.util.logging.LogRecord
import de.fuberlin.wiwiss.silk.util.{CollectLogs, SourceTargetPair}
import de.fuberlin.wiwiss.silk.{OutputTask, FilterTask, MatchTask, LoadTask}
import de.fuberlin.wiwiss.silk.output.{Output, Link}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.util.task.ValueTask

/**
 * Task which executes the current link specification and allows querying for the generated links.
 */
class GenerateLinksTask(user: User, linkSpec: LinkSpecification) extends ValueTask[Seq[Link]](Seq.empty) {
  /***/
  var output: Option[Output] = None

  /** The number of concurrent threads used for matching */
  private val numThreads = 8

  /** Generated links with detailed information */
  private val generateDetailedLinks = true

  /** The size of the instance partitions in the cache */
  private val partitionSize = 300

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
    val project = user.project
    val instanceSpecs = InstanceSpecification.retrieve(linkSpec)

    warningLog = CollectLogs() {
      //Retrieve sources
      val sources = linkSpec.datasets.map(_.sourceId).map(project.sourceModule.task(_).source)

      //Blocking function
      val blockCount = project.linkingModule.config.blocking.map(_.blocks).getOrElse(1)
      def indexFunction(instance: Instance) = linkSpec.condition.index(instance, 0.0)

      //Instance caches
      val caches = SourceTargetPair(new MemoryInstanceCache(instanceSpecs.source, blockCount, partitionSize),
                                    new MemoryInstanceCache(instanceSpecs.target, blockCount, partitionSize))

      //Create tasks
      loadTask = new LoadTask(sources, caches, instanceSpecs, if (blockCount > 0) Some(indexFunction _) else None)
      matchTask = new MatchTask(linkSpec, caches, numThreads, false, generateDetailedLinks)

      //Load instances
      loadTask.runInBackground()

      //Execute matching
      val links = executeSubValueTask(matchTask, 0.95)

      //Filter links
      val filterTask = new FilterTask(links, linkSpec.filter)
      value.update(executeSubTask(filterTask))

      //Output links
      for(out <- output) {
        val outputTask = new OutputTask(value.get, linkSpec.linkType, out :: Nil)
        executeSubTask(outputTask)
      }
    }

    //Return generated links
    value.get
  }

  override def stopExecution() {
    if (loadTask != null) loadTask.cancel()
    if (matchTask != null) matchTask.cancel()
  }
}

object GenerateLinksTask {
  def empty = new GenerateLinksTask(User(), LinkSpecification())
}