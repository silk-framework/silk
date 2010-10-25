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
class LoadTask(config : Configuration, linkSpec : LinkSpecification,
               sourceCache : Option[InstanceCache], targetCache : Option[InstanceCache]) extends Task[Unit]
{
  private val instanceSpecs = InstanceSpecification.retrieve(config, linkSpec)

  private val logger = Logger.getLogger(classOf[LoadTask].getName)

  override def execute()
  {
    val startTime = System.currentTimeMillis()

    for(cache <- sourceCache)
    {
      writeSourceCache(cache)
      cache.close()
    }

    for(cache <- targetCache)
    {
      writeTargetCache(cache)
      cache.close()
    }

    //TODO separate report for source and target cache
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
    sourceCache.clear()
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
    targetCache.clear()
    targetCache.write(instances, linkSpec.blocking)
  }
}
