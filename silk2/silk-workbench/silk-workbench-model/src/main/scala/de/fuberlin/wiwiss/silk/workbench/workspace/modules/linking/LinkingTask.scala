package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.evaluation.Alignment
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}
import de.fuberlin.wiwiss.silk.util.{SourceTargetPair, Task, Identifier}
import de.fuberlin.wiwiss.silk.workbench.instancespec.RelevantPropertiesCollector
import de.fuberlin.wiwiss.silk.util.sparql.{RemoteSparqlEndpoint, SparqlEndpoint, InstanceRetriever}
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import java.net.URI

/**
 * A linking task which interlinks two datasets.
 */
case class LinkingTask(name : Identifier,
                       prefixes : Prefixes,
                       linkSpec : LinkSpecification,
                       alignment : Alignment,
                       cache : Cache) extends ModuleTask
{
  val cacheLoader : Task[Unit] = new CacheLoader()
  cacheLoader.runInBackground()

  private class CacheLoader() extends Task[Unit]
  {
    private val sampleCount = 10000

    private val positiveSamples = alignment.positiveLinks.take(sampleCount).toList

    private val negativeSamples = alignment.negativeLinks.take(sampleCount).toList

    taskName = "CacheLoaderTask"

    override protected def execute()
    {
      //Retrieve sources
      val sourceSource = User().project.sourceModule.tasks.find(_.name == linkSpec.datasets.source.sourceId).get.source
      val targetSource = User().project.sourceModule.tasks.find(_.name == linkSpec.datasets.target.sourceId).get.source

      val sourceEndpoint = new RemoteSparqlEndpoint(new URI(sourceSource.dataSource.toString), prefixes)
      val targetEndpoint = new RemoteSparqlEndpoint(new URI(targetSource.dataSource.toString), prefixes)

      if(cache.instanceSpecs == null)
      {
        updateStatus("Retrieving frequent property paths", 0.0)
        val sourcePaths = RelevantPropertiesCollector(sourceEndpoint, linkSpec.datasets.source.restriction).map(_._1).toSeq
        val targetPaths = RelevantPropertiesCollector(targetEndpoint, linkSpec.datasets.target.restriction).map(_._1).toSeq

        val sourceInstanceSpec = new InstanceSpecification(Constants.SourceVariable, linkSpec.datasets.source.restriction, sourcePaths, prefixes)
        val targetInstanceSpec = new InstanceSpecification(Constants.TargetVariable, linkSpec.datasets.target.restriction, targetPaths, prefixes)

        cache.instanceSpecs = new SourceTargetPair(sourceInstanceSpec, targetInstanceSpec)
      }

      if(!positiveSamples.isEmpty && (cache.positiveInstances == null || cache.negativeInstances == null))
      {
        updateStatus(0.2)

        //Create instance loading tasks
        val positiveSourceInstancesTask = new LoadingInstancesTask(sourceEndpoint, cache.instanceSpecs.source, positiveSamples.map(_.sourceUri))
        val positiveTargetInstancesTask = new LoadingInstancesTask(targetEndpoint, cache.instanceSpecs.target, positiveSamples.map(_.targetUri))

        val negativeSourceInstancesTask =  new LoadingInstancesTask(sourceEndpoint, cache.instanceSpecs.source, negativeSamples.map(_.sourceUri))
        val negativeTargetInstancesTask =  new LoadingInstancesTask(targetEndpoint, cache.instanceSpecs.target, negativeSamples.map(_.targetUri))

        //Load instances
        val positiveSourceInstances = executeSubTask(positiveSourceInstancesTask, 0.4)
        val positiveTargetInstances = executeSubTask(positiveTargetInstancesTask, 0.6)

        val negativeSourceInstances = executeSubTask(negativeSourceInstancesTask, 0.8)
        val negativeTargetInstances = executeSubTask(negativeTargetInstancesTask, 1.0)

        //Fill the cache with the loaded instances
        cache.positiveInstances = (positiveSourceInstances zip positiveTargetInstances).map(SourceTargetPair.fromPair)
        cache.negativeInstances = (negativeSourceInstances zip negativeTargetInstances).map(SourceTargetPair.fromPair)
      }
    }
  }

  /**
   * Task which loads a list of instances from an endpoint.
   */
  private class LoadingInstancesTask(endpoint : SparqlEndpoint, instanceSpec : InstanceSpecification, instanceUrls : Seq[String]) extends Task[List[Instance]]
  {
    override def execute() =
    {
      val instanceTraversable = InstanceRetriever(endpoint).retrieve(instanceSpec, instanceUrls)

      var instanceList : List[Instance] = Nil
      var instanceListSize = 0
      val instanceCount = instanceUrls.size

      updateStatus("Retrieving instances", 0.0)
      for(instance <- instanceTraversable)
      {
        instanceList ::= instance
        instanceListSize += 1
        updateStatus(instanceListSize.toDouble / instanceCount)
      }

      instanceList.reverse
    }
  }
}