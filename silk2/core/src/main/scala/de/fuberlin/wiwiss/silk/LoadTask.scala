package de.fuberlin.wiwiss.silk

import config.Configuration
import datasource.Source
import instance.{Instance, InstanceSpecification, InstanceCache}
import java.util.logging.Logger
import linkspec.LinkSpecification
import util.Task

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
  }

  private def writeSourceCache(sourceCache : InstanceCache)
  {
    writeSourceCache(sourceCache, linkSpec.sourceDataset.source)
  }

  private def writeSourceCache(sourceCache : InstanceCache, source : Source)
  {
    val startTime = System.currentTimeMillis()
    logger.info("Loading instances of source dataset")

    val instances = source.retrieve(instanceSpecs.source)
    sourceCache.clear()
    sourceCache.write(instances)
    //sourceCache.write(instances, Some(linkSpec.condition.index))

    logger.info("Loaded instances of source dataset in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
  }

  private def writeTargetCache(targetCache : InstanceCache)
  {
    writeTargetCache(targetCache, linkSpec.targetDataset.source)
  }

  private def writeTargetCache(targetCache : InstanceCache, source : Source)
  {
    val startTime = System.currentTimeMillis()
    logger.info("Loading instances of target dataset")

    val instances = source.retrieve(instanceSpecs.target)
    targetCache.clear()
    targetCache.write(instances)
    //targetCache.write(instances, Some(linkSpec.condition.index))

    logger.info("Loaded instances of target dataset in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
  }
}
