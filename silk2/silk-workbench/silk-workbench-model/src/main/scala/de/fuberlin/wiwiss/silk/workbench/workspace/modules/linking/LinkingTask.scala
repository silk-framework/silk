package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.evaluation.Alignment
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}
import de.fuberlin.wiwiss.silk.workbench.instancespec.RelevantPropertiesCollector
import de.fuberlin.wiwiss.silk.util.sparql.{RemoteSparqlEndpoint, SparqlEndpoint, InstanceRetriever}
import de.fuberlin.wiwiss.silk.workbench.Constants
import java.net.URI
import de.fuberlin.wiwiss.silk.workbench.workspace.Project
import de.fuberlin.wiwiss.silk.util.{Future, SourceTargetPair, Task, Identifier}

/**
 * A linking task which interlinks two datasets.
 */
case class LinkingTask(name : Identifier,
                       linkSpec : LinkSpecification,
                       alignment : Alignment,
                       cache : Cache) extends ModuleTask
{
  var cacheLoading : Future[Unit] = null

  val cacheLoader : Task[Unit] = new CacheLoader()

  def loadCache(project : Project)
  {
    cacheLoader.asInstanceOf[CacheLoader].project = project
    cacheLoading = cacheLoader.runInBackground()
  }

  def reloadCache(project : Project)
  {
    cache.instanceSpecs = null
    cache.instances = null

    loadCache(project)
  }

  private class CacheLoader() extends Task[Unit]
  {
    var project : Project = null

    private val sampleCount = 100

    private val positiveSamples = alignment.positiveLinks.take(sampleCount).toList

    private val negativeSamples = alignment.negativeLinks.take(sampleCount).toList

    taskName = "CacheLoaderTask"

    override protected def execute()
    {
      val sources = linkSpec.datasets.map(ds => project.sourceModule.task(ds.sourceId).source)

      val sourceEndpoint = new RemoteSparqlEndpoint(new URI(sources.source.dataSource.toString))
      val targetEndpoint = new RemoteSparqlEndpoint(new URI(sources.target.dataSource.toString))

      if(cache.instanceSpecs == null)
      {
        updateStatus("Retrieving frequent property paths", 0.0)
        val sourcePaths = RelevantPropertiesCollector(sourceEndpoint, linkSpec.datasets.source.restriction).map(_._1).toSeq
        val targetPaths = RelevantPropertiesCollector(targetEndpoint, linkSpec.datasets.target.restriction).map(_._1).toSeq

        val sourceInstanceSpec = new InstanceSpecification(Constants.SourceVariable, linkSpec.datasets.source.restriction, sourcePaths)
        val targetInstanceSpec = new InstanceSpecification(Constants.TargetVariable, linkSpec.datasets.target.restriction, targetPaths)

        cache.instanceSpecs = new SourceTargetPair(sourceInstanceSpec, targetInstanceSpec)
      }

      if(!positiveSamples.isEmpty && (cache.instances == null))
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
        val positiveInstances = (positiveSourceInstances zip positiveTargetInstances).map(SourceTargetPair.fromPair)
        val negativeInstances = (negativeSourceInstances zip negativeTargetInstances).map(SourceTargetPair.fromPair)
        cache.instances = ReferenceInstances(positiveInstances, negativeInstances)
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