package de.fuberlin.wiwiss.silk

import config.{Configuration, ConfigLoader}
import datasource.{InstanceSpecification, FilePartitionCache, PartitionCache}
import linkspec.LinkSpecification
import output.Link
import java.util.concurrent.{TimeUnit, Executors}
import collection.mutable.{ArrayBuffer, SynchronizedBuffer}
import java.io.File
import java.util.logging.{Level, Logger}

object Silk
{
    def main(args : Array[String])
    {
        val configFile = System.getProperty("configFile") match
        {
            case fileName : String => new File(fileName)
            case _ => throw new IllegalArgumentException("No configuration file specified. Please set the 'configFile' property")
        }

        val config = ConfigLoader.load(configFile)
        val linkSpec = config.linkSpecs.values.head

        val silk = new Silk(config, linkSpec)
        silk.createPartitions()
        silk.generateLinks()
    }
}

class Silk(config : Configuration, linkSpec : LinkSpecification)
{
    private val logger = Logger.getLogger(classOf[Silk].getName)

    private val partitionCacheDir = new File("./partitionCacheTest/")

    private val sourcePartitionCache : PartitionCache = new FilePartitionCache(new File(partitionCacheDir + "/" + linkSpec.sourceDatasetSpecification.dataSource.id + "/"))
    private val targetPartitionCache : PartitionCache = new FilePartitionCache(new File(partitionCacheDir + "/" + linkSpec.targetDatasetSpecification.dataSource.id + "/"))

    def createPartitions()
    {
        //Create instance specifications
        val (sourceInstanceSpec, targetInstanceSpec) = InstanceSpecification.retrieve(linkSpec)
        println(sourceInstanceSpec)
        println(targetInstanceSpec)

        //Retrieve instances
        val sourceInstances = linkSpec.sourceDatasetSpecification.dataSource.retrieve(sourceInstanceSpec, config.prefixes)
        val targetInstances = linkSpec.targetDatasetSpecification.dataSource.retrieve(targetInstanceSpec, config.prefixes)

        logger.info("Creating partitions of " + linkSpec.sourceDatasetSpecification.dataSource.id)
        sourcePartitionCache.write(sourceInstances)
        
        logger.info("Creating partitions of " + linkSpec.targetDatasetSpecification.dataSource.id)
        targetPartitionCache.write(targetInstances)
    }

    def generateLinks()
    {
        logger.info("Generating links")

        val startTime = System.currentTimeMillis()

        //Check if any partitions have been found
        if(sourcePartitionCache.size == 0 || targetPartitionCache.size == 0)
        {
            logger.warning("No partitions found in " + partitionCacheDir)
        }

        //Execute match tasks
        val executor = Executors.newFixedThreadPool(4)
        val linkBuffer = new ArrayBuffer[Link]() with SynchronizedBuffer[Link]

        for(sourcePartitionIndex <- 0 until sourcePartitionCache.size;
            targetPartitionIndex <- 0 until targetPartitionCache.size)
        {
            executor.submit(new MatchTask(sourcePartitionIndex, targetPartitionIndex, link => linkBuffer.append(link)))
        }

        executor.shutdown()
        executor.awaitTermination(1000, TimeUnit.DAYS)

        //Write output
        linkSpec.outputs.foreach(_.open)

        if(linkSpec.filter.limit.isDefined)
        {
            logger.info("Filtering output")

            //Apply filter
            for((sourceUri, links) <- linkBuffer.groupBy(_.sourceUri))
            {
                val bestLinks = links.sortWith(_.confidence > _.confidence).take(linkSpec.filter.limit.get)

                for(link <- bestLinks) linkSpec.outputs.foreach(_.write(link, linkSpec.linkType))
            }
        }
        else
        {
            for(link <- linkBuffer) linkSpec.outputs.foreach(_.write(link, linkSpec.linkType))
        }

        linkSpec.outputs.foreach(_.close)

        logger.info("Generated links in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
    }

    private class MatchTask(sourcePartitionIndex : Int, targetPartitionIndex : Int, callback : Link => Unit) extends Runnable
    {
        override def run() : Unit =
        {
            try
            {
                val taskNum = (sourcePartitionIndex * targetPartitionCache.size + targetPartitionIndex) + 1
                val taskCount = sourcePartitionCache.size * targetPartitionCache.size
                logger.info("Starting task " + taskNum + " of " + taskCount)

                for(sourceInstance <- sourcePartitionCache(sourcePartitionIndex);
                    targetInstance <- targetPartitionCache(targetPartitionIndex))
                {
                    val confidence = linkSpec.condition.evaluate(sourceInstance, targetInstance).headOption.getOrElse(0.0)

                    if(confidence >= linkSpec.filter.threshold)
                    {
                        callback(new Link(sourceInstance.uri, targetInstance.uri, confidence))
                    }
                }

                logger.info("Completed match task " + taskNum + " of " + taskCount)
            }
            catch
            {
                case ex : Exception => logger.log(Level.WARNING, "Could not execute match task", ex)
            }
        }
    }
}
