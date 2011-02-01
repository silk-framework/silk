package de.fuberlin.wiwiss.silk.workbench.project

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.output.Link
import java.io._
import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec._
import de.fuberlin.wiwiss.silk.instance.{InstanceCache, InstanceSpecification, MemoryInstanceCache}
import de.fuberlin.wiwiss.silk.util.Task
import de.fuberlin.wiwiss.silk.evaluation.Alignment
import de.fuberlin.wiwiss.silk.workbench.instancespec.{RelevantPropertiesCollector}
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.util.sparql.{InstanceRetriever, ParallelInstanceRetriever, RemoteSparqlEndpoint, SparqlEndpoint}

case class Project(desc : SourceTargetPair[Description],
                   config : Configuration,//TODO remove and hold all variables in desc
                   linkSpec : LinkSpecification,
                   alignment : Alignment,
                   cache : Cache)
{
  //TODO validate

  private val logger = Logger.getLogger(getClass.getName)

  val cacheLoader : Task[Unit] = new CacheLoader()

  cacheLoader.runInBackground()

  private class CacheLoader() extends Task[Unit]
  {
    private val sampleCount = 100

    private val positiveSamples = alignment.positiveLinks.take(sampleCount).toList

    private val negativeSamples = alignment.negativeLinks.take(sampleCount).toList

    taskName = "CacheLoaderTask"

    override protected def execute()
    {
      val sourceEndpoint = new RemoteSparqlEndpoint(desc.source.endpointUri, config.prefixes)
      val targetEndpoint = new RemoteSparqlEndpoint(desc.target.endpointUri, config.prefixes)

      if(cache.instanceSpecs == null)
      {
        updateStatus("Retrieving frequent property paths", 0.2)
        val sourcePaths = RelevantPropertiesCollector(sourceEndpoint, desc.source.restriction).map(_._1).toSeq
        val targetPaths = RelevantPropertiesCollector(targetEndpoint, desc.target.restriction).map(_._1).toSeq

        val sourceInstanceSpec = new InstanceSpecification(Constants.SourceVariable, desc.source.restriction, sourcePaths, config.prefixes)
        val targetInstanceSpec = new InstanceSpecification(Constants.TargetVariable, desc.target.restriction, targetPaths, config.prefixes)

        cache.instanceSpecs = new SourceTargetPair(sourceInstanceSpec, targetInstanceSpec)
        updateStatus("Retrieved frequent property paths", 0.4)
      }

      if(!positiveSamples.isEmpty && (cache.positiveInstances == null || cache.negativeInstances == null))
      {
        updateStatus("Loading instances into cache", 0.6)

        val sourceRetriever = InstanceRetriever(sourceEndpoint)
        val targetRetriever = InstanceRetriever(targetEndpoint)

        val positiveSourceInstances = sourceRetriever.retrieve(cache.instanceSpecs.source, positiveSamples.map(_.sourceUri)).toList
        val positiveTargetInstances = targetRetriever.retrieve(cache.instanceSpecs.target, positiveSamples.map(_.targetUri)).toList

        val negativeSourceInstances = sourceRetriever.retrieve(cache.instanceSpecs.source, negativeSamples.map(_.sourceUri)).toList
        val negativeTargetInstances = targetRetriever.retrieve(cache.instanceSpecs.target, negativeSamples.map(_.targetUri)).toList

        cache.positiveInstances = (positiveSourceInstances zip positiveTargetInstances).map(SourceTargetPair.fromPair)
        cache.negativeInstances = (negativeSourceInstances zip negativeTargetInstances).map(SourceTargetPair.fromPair)

        updateStatus("Instances loaded into cache", 0.8)
      }
    }
  }
}

object Project
{
  private val logger = Logger.getLogger(getClass.getName)

  @volatile private var project : Option[Project] = None

  def isOpen = project.isDefined

  def apply() =
  {
    project match
    {
      case Some(p) => p
      case None => throw new IllegalStateException("No project is open.")
    }
  }

  def create(description : SourceTargetPair[Description], prefixes : Map[String, String])
  {
    project = Some(ProjectCreator.create(description, prefixes))
  }

  def open(inputStream : InputStream)
  {
    project = Some(ProjectReader.read(inputStream))
  }

  def save(ouputStream : OutputStream)
  {
    ProjectWriter.write(Project(), ouputStream)
  }

  def close()
  {
    project = None
  }

  def updateLinkSpec(linkSpec : LinkSpecification)
  {
    val updatedConfig = project.get.config.copy(linkSpecs = linkSpec :: Nil)
    project = Some(project.get.copy(config = updatedConfig, linkSpec = linkSpec, cache = new Cache()))
  }

  def updateAlignment(alignment : Alignment)
  {
    project = Some(project.get.copy(alignment = alignment, cache = new Cache()))
  }
}
