package de.fuberlin.wiwiss.silk.workbench.evaluation

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.util.{SourceTargetPair, Task}
import de.fuberlin.wiwiss.silk.instance.{InstanceCache, Instance, InstanceSpecification, MemoryInstanceCache}
import de.fuberlin.wiwiss.silk.{FilterTask, MatchTask, LoadTask}
import collection.mutable.Buffer
import de.fuberlin.wiwiss.silk.output.Link

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
    private val instanceSpecs = InstanceSpecification.retrieve(linkSpec)

    private val sources = linkSpec.datasets.map(_.sourceId).map(project.sourceModule.task(_).source)

    private def blockingFunction(instance : Instance) = linkSpec.condition.index(instance, linkSpec.filter.threshold).map(_ % blockCount)

    //Instance caches
    private val caches = SourceTargetPair(new MemoryInstanceCache(instanceSpecs.source, blockCount, 300),
                                          new MemoryInstanceCache(instanceSpecs.target, blockCount, 300))

    private val loadTask = new LoadTask(sources, caches, instanceSpecs, if(blockCount > 0) Some(blockingFunction _) else None)

    private val matchTask = new MatchTask(linkingTask.linkSpec, caches, 8)

    private var filteredLinks : Buffer[Link] = null

    def links : Traversable[(Link, Boolean)] =
    {
      if(filteredLinks == null)
      {
        matchTask.links.map(link => (link, false))
      }
      else
      {
        filteredLinks.map(link => (link, false))
      }
    }

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
      filteredLinks = null

      //Load instances
      loadTask.runInBackground()

      //Execute matching
      executeSubTask(matchTask, 0.95)

      //Filter links
      val filterTask = new FilterTask(matchTask.links, linkSpec.filter)
      filteredLinks = executeSubTask(filterTask)
    }

    override def stopExecution()
    {
      loadTask.cancel()
      matchTask.cancel()
    }
  }
}