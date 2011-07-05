package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.instance.{MemoryInstanceCache, Instance, InstanceSpecification}
import collection.mutable.Buffer
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.{FilterTask, MatchTask, LoadTask}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.evaluation.Alignment
import java.util.logging.LogRecord
import de.fuberlin.wiwiss.silk.util.{CollectLogs, SourceTargetPair, Task}

/**
 * Task which executes the current link specification and allows querying for the generated links.
 */
class EvaluationTask(user : User) extends Task[Unit]
{
  /** The number of concurrent threads used for matching */
  private val numThreads = 8

  /** Generated links with detailed information */
  private val generateDetailedLinks = true

  /** The size of the instance partitions in the cache */
  private val partitionSize = 300

  @volatile private var loadTask : LoadTask = null

  @volatile private var matchTask : MatchTask = null

  @volatile private var alignment : Alignment = null

  @volatile private var filteredLinks : Buffer[Link] = null

  @volatile private var warningLog : Seq[LogRecord] = Seq.empty

  /**
   * Retrieves the current links.
   */
  def links : Seq[Link] =
  {
    if(filteredLinks != null)
    {
      filteredLinks
    }
    else if(matchTask != null)
    {
      matchTask.links
    }
    else
    {
      Seq.empty
    }
  }

  /**
   * All warnings which have been generated during executing.
   */
  def warnings = warningLog

  def clear()
  {
    cancel()
    if(matchTask != null) matchTask.links.clear()
    if(filteredLinks != null) filteredLinks.clear()
  }

  override protected def execute()
  {
    val project = user.project
    val linkingTask = user.linkingTask
    val linkSpec = linkingTask.linkSpec
    val instanceSpecs = InstanceSpecification.retrieve(linkSpec)

    warningLog = CollectLogs()
    {
      //Retrieve sources
      val sources = linkSpec.datasets.map(_.sourceId).map(project.sourceModule.task(_).source)

      //Blocking function
      val blockCount = project.linkingModule.config.blocking.map(_.blocks).getOrElse(1)
      def blockingFunction(instance : Instance) = linkSpec.condition.index(instance).map(_ % blockCount)

      //Instance caches
      val caches = SourceTargetPair(new MemoryInstanceCache(instanceSpecs.source, blockCount, partitionSize),
                                    new MemoryInstanceCache(instanceSpecs.target, blockCount, partitionSize))

      //Create tasks
      loadTask = new LoadTask(sources, caches, instanceSpecs, if(blockCount > 0) Some(blockingFunction _) else None)
      matchTask = new MatchTask(linkingTask.linkSpec, caches, numThreads, false, generateDetailedLinks)
      alignment = linkingTask.alignment
      filteredLinks = null

      //Load instances
      loadTask.runInBackground()

      //Execute matching
      executeSubTask(matchTask, 0.95)

      //Filter links
      val filterTask = new FilterTask(matchTask.links, linkSpec.filter)
      filteredLinks = executeSubTask(filterTask)
    }
  }

  override def stopExecution()
  {
    if(loadTask != null) loadTask.cancel()
    if(matchTask != null) matchTask.cancel()
  }
}