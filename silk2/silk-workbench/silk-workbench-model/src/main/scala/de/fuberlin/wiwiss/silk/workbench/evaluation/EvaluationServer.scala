package de.fuberlin.wiwiss.silk.workbench.evaluation

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.{MatchTask, LoadTask}
import de.fuberlin.wiwiss.silk.util.Task
import de.fuberlin.wiwiss.silk.instance.{Instance, InstanceSpecification, MemoryInstanceCache}
import de.fuberlin.wiwiss.silk.workbench.workspace.User

object EvaluationServer
{
  private val logger = Logger.getLogger(getClass.getName)

  private val task = new EvaluationTask()

  val evaluationTask : Task[Unit] = task

  def links = task.links

  private class EvaluationTask extends Task[Unit]
  {
    private val project = User().project
    private val linkingTask = User().linkingTask
    private val linkSpec = linkingTask.linkSpec
    private val blockCount = project.linkingModule.config.blocking.map(_.blocks).getOrElse(1)

    //Instance caches
    private val sourceCache = new MemoryInstanceCache(blockCount, 100)
    private val targetCache = new MemoryInstanceCache(blockCount, 100)

    private val matchTask = new MatchTask(linkingTask.linkSpec, sourceCache, targetCache, 8)

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
      while(User().linkingTask.cacheLoader.isRunning)
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
      //Retrieve sources
      val sourceSource = project.sourceModule.tasks.find(_.name == linkSpec.datasets.source.sourceId).get.source
      val targetSource = project.sourceModule.tasks.find(_.name == linkSpec.datasets.target.sourceId).get.source

      //Retrieve Instance Specifications from Link Specification
      val instanceSpecs = InstanceSpecification.retrieve(linkSpec, linkingTask.prefixes)

      def blockingFunction(instance : Instance) = linkSpec.condition.index(instance, linkSpec.filter.threshold).map(_ % blockCount)

      //Load instances into cache
      val loadSourceCacheTask = new LoadTask(sourceSource, sourceCache, instanceSpecs.source, if(blockCount > 0) Some(blockingFunction _) else None)
      val loadTargetCacheTask = new LoadTask(targetSource, targetCache, instanceSpecs.target, if(blockCount > 0) Some(blockingFunction _) else None)

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