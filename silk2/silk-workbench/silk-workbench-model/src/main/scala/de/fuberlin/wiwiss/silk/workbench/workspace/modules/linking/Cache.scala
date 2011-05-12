package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}
import xml.{NodeBuffer, Node}
import de.fuberlin.wiwiss.silk.util.sparql.{InstanceRetriever, SparqlEndpoint}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.workbench.workspace.Project
import de.fuberlin.wiwiss.silk.workbench.Constants
import collection.immutable.List._
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.evaluation.{Alignment, ReferenceInstances}
import de.fuberlin.wiwiss.silk.util.{Future, Task, SourceTargetPair}

//TODO use options?
//TODO store path frequencies
class Cache(var instanceSpecs : SourceTargetPair[InstanceSpecification] = null,
            var instances : ReferenceInstances = ReferenceInstances.empty)
{
  var isLoading : Future[Unit] = null

  val loader : Task[Unit] = new CacheLoader()

  def load(project : Project, linkingTask : LinkingTask)
  {
    if(!loader.isRunning)
    {
      loader.asInstanceOf[CacheLoader].project = project
      loader.asInstanceOf[CacheLoader].alignment = linkingTask.alignment
      loader.asInstanceOf[CacheLoader].linkSpec = linkingTask.linkSpec

      isLoading = loader.runInBackground()
    }
  }

  def reload(project : Project, linkingTask : LinkingTask)
  {
    instanceSpecs = null
    instances = ReferenceInstances.empty

    load(project, linkingTask)
  }

  def toXML(implicit prefixes : Prefixes) : Node =
  {
    val nodes = new NodeBuffer()

    if(instanceSpecs != null)
    {
      nodes.append(
        <InstanceSpecifications>
          <Source>
            { instanceSpecs.source.toXML }
          </Source>
          <Target>
            { instanceSpecs.target.toXML }
          </Target>
        </InstanceSpecifications>)
    }

    nodes.append(
        <PositiveInstances>{
        for(SourceTargetPair(sourceInstance, targetInstance) <- instances.positive.values) yield
        {
          <Pair>
            <Source>{sourceInstance.toXML}</Source>
            <Target>{targetInstance.toXML}</Target>
          </Pair>
        }
        }</PositiveInstances>)

    nodes.append(
      <NegativeInstances>{
        for(SourceTargetPair(sourceInstance, targetInstance) <- instances.negative.values) yield
        {
          <Pair>
            <Source>{sourceInstance.toXML}</Source>
            <Target>{targetInstance.toXML}</Target>
          </Pair>
        }
      }</NegativeInstances>)

    <Cache>{nodes}</Cache>
  }

  private class CacheLoader() extends Task[Unit]
  {
    var alignment : Alignment = null

    var linkSpec : LinkSpecification = null

    var project : Project = null

    private val sampleCount = 2000

    taskName = "CacheLoaderTask"

    override protected def execute()
    {
      val sources = linkSpec.datasets.map(ds => project.sourceModule.task(ds.sourceId).source)

      if(instanceSpecs == null)
      {
        updateStatus("Retrieving frequent property paths", 0.0)
        val sourcePaths = sources.source.dataSource.retrievePaths(linkSpec.datasets.source.restriction, 1, Some(50))
        val targetPaths = sources.target.dataSource.retrievePaths(linkSpec.datasets.target.restriction, 1, Some(50))

        val sourceInstanceSpec = new InstanceSpecification(Constants.SourceVariable, linkSpec.datasets.source.restriction, sourcePaths.map(_._1).toSeq)
        val targetInstanceSpec = new InstanceSpecification(Constants.TargetVariable, linkSpec.datasets.target.restriction, targetPaths.map(_._1).toSeq)

        instanceSpecs = new SourceTargetPair(sourceInstanceSpec, targetInstanceSpec)
      }

      updateStatus(0.2)

      val positiveSamples = alignment.positive.take(sampleCount).toList

      val negativeSamples = alignment.negative.take(sampleCount).toList

      //Determine which instances are already in the cache
      val existingPositiveInstances = positiveSamples.map(instances.positive.get).flatten
      val existingNegativeInstances = negativeSamples.map(instances.negative.get).flatten

      //Determine which instances are missing in the cache
      val missingPositiveInstances = positiveSamples.filterNot(instances.positive.contains)
      val missingNegativeInstances = negativeSamples.filterNot(instances.negative.contains)

      //Create instance loading tasks
      val positiveSourceInstancesTask = new LoadingInstancesTask(sources.source.dataSource, instanceSpecs.source, missingPositiveInstances.map(_.sourceUri))
      val positiveTargetInstancesTask = new LoadingInstancesTask(sources.target.dataSource, instanceSpecs.target, missingPositiveInstances.map(_.targetUri))

      val negativeSourceInstancesTask =  new LoadingInstancesTask(sources.source.dataSource, instanceSpecs.source, missingNegativeInstances.map(_.sourceUri))
      val negativeTargetInstancesTask =  new LoadingInstancesTask(sources.target.dataSource, instanceSpecs.target, missingNegativeInstances.map(_.targetUri))

      //Load instances
      val newPositiveSourceInstances = executeSubTask(positiveSourceInstancesTask, 0.4)
      val newPositiveTargetInstances = executeSubTask(positiveTargetInstancesTask, 0.6)

      val newNegativeSourceInstances = executeSubTask(negativeSourceInstancesTask, 0.8)
      val newNegativeTargetInstances = executeSubTask(negativeTargetInstancesTask, 1.0)

      val newPositiveInstances = (newPositiveSourceInstances zip newPositiveTargetInstances).map(SourceTargetPair.fromPair)
      val newNegativeInstances = (newNegativeSourceInstances zip newNegativeTargetInstances).map(SourceTargetPair.fromPair)

      //Update cache
      instances = ReferenceInstances.fromInstances(existingPositiveInstances ++ newPositiveInstances, existingNegativeInstances ++ newNegativeInstances)
    }
  }

  /**
   * Task which loads a list of instances from an endpoint.
   */
  private class LoadingInstancesTask(source : DataSource, instanceSpec : InstanceSpecification, instanceUrls : Seq[String]) extends Task[List[Instance]]
  {
    override def execute() =
    {
      if(instanceUrls.isEmpty)
      {
        List[Instance]()
      }
      else
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
}

object Cache
{
  def fromXML(node : Node) : Cache =
  {
    val instanceSpecs =
    {
      if(node \ "InstanceSpecifications" isEmpty)
      {
        null
      }
      else
      {
        val sourceSpec = InstanceSpecification.fromXML(node \ "InstanceSpecifications" \ "Source" \ "_" head)
        val targetSpec = InstanceSpecification.fromXML(node \ "InstanceSpecifications" \ "Target" \ "_" head)
        new SourceTargetPair(sourceSpec, targetSpec)
      }
    }

    val positiveInstances : Traversable[SourceTargetPair[Instance]] =
    {
      if(node \ "PositiveInstances" isEmpty)
      {
        Traversable.empty
      }
      else
      {
        for(pairNode <- node \ "PositiveInstances" \ "Pair" toList) yield
        {
           SourceTargetPair(
             Instance.fromXML(pairNode \ "Source" \ "Instance" head, instanceSpecs.source),
             Instance.fromXML(pairNode \ "Target" \ "Instance" head, instanceSpecs.target))
        }
      }
    }

    val negativeInstances : Traversable[SourceTargetPair[Instance]] =
    {
      if(node \ "NegativeInstances" isEmpty)
      {
        Traversable.empty
      }
      else
      {
        for(pairNode <- node \ "NegativeInstances" \ "Pair" toList) yield
        {
           SourceTargetPair(
             Instance.fromXML(pairNode \ "Source" \ "Instance" head, instanceSpecs.source),
             Instance.fromXML(pairNode \ "Target" \ "Instance" head, instanceSpecs.target))
        }
      }
    }

    new Cache(instanceSpecs, ReferenceInstances.fromInstances(positiveInstances, negativeInstances))
  }
}
