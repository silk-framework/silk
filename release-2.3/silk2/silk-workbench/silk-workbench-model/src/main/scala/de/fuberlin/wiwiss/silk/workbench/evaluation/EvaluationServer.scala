package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.output.Link
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.{MatchTask, LoadTask}
import de.fuberlin.wiwiss.silk.instance.MemoryInstanceCache
import de.fuberlin.wiwiss.silk.util.Task
import de.fuberlin.wiwiss.silk.workbench.project.Project

object EvaluationServer
{
  private val logger = Logger.getLogger(getClass.getName)

  private val task = new EvaluationTask()

  val evaluationTask : Task[Unit] = task

  def links = task.links

  private class EvaluationTask extends Task[Unit]
  {
    //Instance caches
    private val sourceCache = new MemoryInstanceCache(Project().config.blocking.map(_.blocks).getOrElse(1), 100)
    private val targetCache = new MemoryInstanceCache(Project().config.blocking.map(_.blocks).getOrElse(1), 100)

    private val matchTask = new MatchTask(Project().config, Project().linkSpec, sourceCache, targetCache, 8)

    def links = matchTask.links.map(link => (link, false))

    override def execute() =
    {
      //waitForCache()

      //val alignmentSet = Project().alignment.toSet

      //generateAlignmentLinks().map(link => (link, alignmentSet.contains(link)))

      generateAllLinks()
    }

    private def waitForCache()
    {
      updateStatus("Waiting until instance cache is loaded", 0.0)
      while(Project().cacheLoader.isRunning)
      {
        Thread.sleep(100)
      }
    }

//    private def generateAlignmentLinks() =
//    {
//      updateStatus("Generating links", 0.5)
//
//      val sourceInstances = Project().cache.instanceCaches.source.read(0, 0)
//      val targetInstances = Project().cache.instanceCaches.target.read(0, 0)
//
//      var generatedLinks = List[Link]()
//      for((sourceInstance, targetInstance) <- sourceInstances zip targetInstances)
//      {
//        val confidence = Project().linkSpec.condition(sourceInstance, targetInstance)
//        if(confidence > Project().linkSpec.filter.threshold)
//        {
//          generatedLinks ::= new Link(sourceInstance.uri, targetInstance.uri, confidence)
//        }
//      }
//
//      generatedLinks
//    }

    private def generateAllLinks() =
    {
      //Load instances into cache
      val loadSourceCacheTask = new LoadTask(Project().config, Project().linkSpec, Some(sourceCache), None)
      val loadTargetCacheTask = new LoadTask(Project().config, Project().linkSpec, None, Some(targetCache))

      loadSourceCacheTask.runInBackground()
      loadTargetCacheTask.runInBackground()

      //Wait until caches are being written
      while((loadSourceCacheTask.isRunning && !sourceCache.isWriting) || (loadTargetCacheTask.isRunning && !targetCache.isWriting))
      {
        Thread.sleep(100)
      }

      //Execute matching
      executeSubTask(matchTask)
    }
  }
}