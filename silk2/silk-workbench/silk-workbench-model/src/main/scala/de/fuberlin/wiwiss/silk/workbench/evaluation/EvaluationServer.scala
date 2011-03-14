package de.fuberlin.wiwiss.silk.workbench.evaluation

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.{MatchTask, LoadTask}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.util.{SourceTargetPair, Task}
import de.fuberlin.wiwiss.silk.instance.{InstanceCache, Instance, InstanceSpecification, MemoryInstanceCache}

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
    val caches = SourceTargetPair.fill[InstanceCache](new MemoryInstanceCache(blockCount, 100))

    private val matchTask = new MatchTask(linkingTask.linkSpec, caches, 8)

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
      val sources = linkSpec.datasets.map(_.sourceId).map(project.sourceModule.task(_).source)

      //Retrieve Instance Specifications from Link Specification
      val instanceSpecs = InstanceSpecification.retrieve(linkSpec)

      def blockingFunction(instance : Instance) = linkSpec.condition.index(instance, linkSpec.filter.threshold).map(_ % blockCount)

      //Load instances into cache
      val loadTask = new LoadTask(sources, caches, instanceSpecs, if(blockCount > 0) Some(blockingFunction _) else None)
      loadTask.runInBackground()

      //Execute matching
      executeSubTask(matchTask)
    }
  }
}