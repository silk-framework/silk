package de.fuberlin.wiwiss.silk

import datasource.Source
import instance.{Instance, InstanceSpecification, InstanceCache}
import java.util.logging.Logger
import util.Task

/**
 * Loads the instance cache
 */
//TODO remove blockingFunction argument by integrating it into instance cache
class LoadTask(source : Source,
               instanceCache : InstanceCache,
               instanceSpec : InstanceSpecification,
               blockingFunction : Option[Instance => Set[Int]] = None) extends Task[Unit]
{
  private val logger = Logger.getLogger(classOf[LoadTask].getName)

  override def execute()
  {
    val startTime = System.currentTimeMillis()
    logger.info("Loading instances of source dataset")

    instanceCache.clear()
    instanceCache.write(source.retrieve(instanceSpec), blockingFunction)
    instanceCache.close()

    logger.info("Loaded instances of source dataset in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
  }
}
