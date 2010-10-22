package de.fuberlin.wiwiss.silk

import config.Configuration
import datasource.Source
import instance.{InstanceSpecification, InstanceCache}
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.workbench.Task
import linkspec.LinkSpecification

/**
 * Loads the instance cache
 */
class LoadTask(config : Configuration, linkSpec : LinkSpecification, sourceCache : InstanceCache, targetCache : InstanceCache) extends Task[Unit]
{
  private val instanceSpecs = InstanceSpecification.retrieve(config, linkSpec)

  private val logger = Logger.getLogger(classOf[LoadTask].getName)

  override def execute()
  {
    val startTime = System.currentTimeMillis()
    logger.info("Loading instances")

    //TODO read in parallel
    writeSourceCache(sourceCache)
    writeTargetCache(targetCache)

    logger.info("Loaded instances in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
  }

  private def writeSourceCache(sourceCache : InstanceCache)
  {
    writeSourceCache(sourceCache, linkSpec.sourceDataset.source)
  }

  private def writeSourceCache(sourceCache : InstanceCache, source : Source)
  {
    val instances = source.retrieve(instanceSpecs.source)

    logger.info("Loading instances of source dataset")
    sourceCache.write(instances, linkSpec.blocking)
  }

  private def writeTargetCache(targetCache : InstanceCache)
  {
    writeTargetCache(targetCache, linkSpec.targetDataset.source)
  }

  private def writeTargetCache(targetCache : InstanceCache, source : Source)
  {
    val instances = source.retrieve(instanceSpecs.target)

    logger.info("Loading instances of target dataset")
    targetCache.write(instances, linkSpec.blocking)
  }
}
