package de.fuberlin.wiwiss.silk.workbench.project

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.output.Link
import java.io._
import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec._
import de.fuberlin.wiwiss.silk.util.sparql.{SparqlEndpoint, InstanceRetriever}
import de.fuberlin.wiwiss.silk.workbench.instancespec.InstanceSpecBuilder
import de.fuberlin.wiwiss.silk.instance.{InstanceCache, InstanceSpecification, MemoryInstanceCache}
import de.fuberlin.wiwiss.silk.util.Task
import de.fuberlin.wiwiss.silk.evaluation.Alignment

case class Project(desc : SourceTargetPair[Description],
                   config : Configuration,
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
    private val sampleCount = 100 //TODO

    private val positiveSamples = alignment.positiveLinks.take(sampleCount).toList

    private val negativeSamples = alignment.negativeLinks.take(sampleCount).toList

    override protected def execute()
    {
      if(cache.instanceSpecs == null)
      {
        updateStatus("Generating instance specifications", 0.2)
        cache.instanceSpecs = new InstanceSpecBuilder(desc.source.endpoint, desc.target.endpoint, positiveSamples, config.prefixes)()
        updateStatus("Generated instance specifications", 0.4)
      }

      if(cache.positiveInstances == null || cache.negativeInstances == null)
      {
        updateStatus("Loading instances into cache", 0.6)

        val sourceRetriever = new InstanceRetriever(desc.source.endpoint)
        val targetRetriever = new InstanceRetriever(desc.target.endpoint)

        val positiveSourceInstances = sourceRetriever.retrieveList(positiveSamples.map(_.sourceUri), cache.instanceSpecs.source).toList
        val positiveTargetInstances = targetRetriever.retrieveList(positiveSamples.map(_.targetUri), cache.instanceSpecs.target).toList

        val negativeSourceInstances = sourceRetriever.retrieveList(negativeSamples.map(_.sourceUri), cache.instanceSpecs.source).toList
        val negativeTargetInstances = targetRetriever.retrieveList(negativeSamples.map(_.targetUri), cache.instanceSpecs.target).toList

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

  def create(description : SourceTargetPair[Description])
  {
    project = Some(ProjectCreator.create(description))
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

  def updateConfig(config : Configuration)
  {
    project = Some(project.get.copy(config = config, linkSpec = config.linkSpecs.head, cache = new Cache()))
  }

  def updateAlignment(alignment : Alignment)
  {
    project = Some(project.get.copy(alignment = alignment, cache = new Cache()))
  }
}
