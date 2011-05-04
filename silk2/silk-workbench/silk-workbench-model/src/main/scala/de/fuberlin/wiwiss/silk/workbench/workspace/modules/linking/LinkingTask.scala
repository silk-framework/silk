package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.workbench.workspace.Project
import de.fuberlin.wiwiss.silk.util.{Future, SourceTargetPair, Task, Identifier}
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceInstances, Alignment}

/**
 * A linking task which interlinks two datasets.
 */
case class LinkingTask(linkSpec : LinkSpecification,
                       alignment : Alignment = Alignment(),
                       cache : Cache = new Cache) extends ModuleTask
{
  val name = linkSpec.id

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

    private val sampleCount = 2000

    private val positiveSamples = alignment.positiveLinks.take(sampleCount).toList

    private val negativeSamples = alignment.negativeLinks.take(sampleCount).toList

    taskName = "CacheLoaderTask"

    override protected def execute()
    {
      val sources = linkSpec.datasets.map(ds => project.sourceModule.task(ds.sourceId).source)

      if(cache.instanceSpecs == null)
      {
        updateStatus("Retrieving frequent property paths", 0.0)
        val sourcePaths = sources.source.dataSource.retrievePaths(linkSpec.datasets.source.restriction, 1, Some(50))
        val targetPaths = sources.target.dataSource.retrievePaths(linkSpec.datasets.target.restriction, 1, Some(50))

        val sourceInstanceSpec = new InstanceSpecification(Constants.SourceVariable, linkSpec.datasets.source.restriction, sourcePaths.map(_._1).toSeq)
        val targetInstanceSpec = new InstanceSpecification(Constants.TargetVariable, linkSpec.datasets.target.restriction, targetPaths.map(_._1).toSeq)

        cache.instanceSpecs = new SourceTargetPair(sourceInstanceSpec, targetInstanceSpec)
      }

      if(!positiveSamples.isEmpty && (cache.instances == null))
      {
        updateStatus(0.2)

        //Determine which instances are already in the cache
        val existingPositiveInstances = positiveSamples.map(cache.instances.positive.get).flatten
        val existingNegativeInstances = negativeSamples.map(cache.instances.negative.get).flatten

        //Determine which instances are missing in the cache
        val missingPositiveInstances = positiveSamples.filterNot(cache.instances.positive.contains)
        val missingNegativeInstances = negativeSamples.filterNot(cache.instances.negative.contains)

        //Create instance loading tasks
        val positiveSourceInstancesTask = new LoadingInstancesTask(sources.source.dataSource, cache.instanceSpecs.source, missingPositiveInstances.map(_.sourceUri))
        val positiveTargetInstancesTask = new LoadingInstancesTask(sources.target.dataSource, cache.instanceSpecs.target, missingPositiveInstances.map(_.targetUri))

        val negativeSourceInstancesTask =  new LoadingInstancesTask(sources.source.dataSource, cache.instanceSpecs.source, missingNegativeInstances.map(_.sourceUri))
        val negativeTargetInstancesTask =  new LoadingInstancesTask(sources.target.dataSource, cache.instanceSpecs.target, missingNegativeInstances.map(_.targetUri))

        //Load instances
        val newPositiveSourceInstances = executeSubTask(positiveSourceInstancesTask, 0.4)
        val newPositiveTargetInstances = executeSubTask(positiveTargetInstancesTask, 0.6)

        val newNegativeSourceInstances = executeSubTask(negativeSourceInstancesTask, 0.8)
        val newNegativeTargetInstances = executeSubTask(negativeTargetInstancesTask, 1.0)

        val newPositiveInstances = (newPositiveSourceInstances zip newPositiveTargetInstances).map(SourceTargetPair.fromPair)
        val newNegativeInstances = (newNegativeSourceInstances zip newNegativeTargetInstances).map(SourceTargetPair.fromPair)

        //Update cache
        cache.instances = ReferenceInstances.fromInstances(existingPositiveInstances ++ newPositiveInstances, existingNegativeInstances ++ newNegativeInstances)
      }
    }
  }

  /**
   * Task which loads a list of instances from an endpoint.
   */
  private class LoadingInstancesTask(source : DataSource, instanceSpec : InstanceSpecification, instanceUrls : Seq[String]) extends Task[List[Instance]]
  {
    override def execute() =
    {
      val instanceTraversable = source.retrieve(instanceSpec, instanceUrls)

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