package de.fuberlin.wiwiss.silk

import datasource.{InstanceSpecification, FilePartitionCache, PartitionCache}
import linkspec.{Configuration, ConfigLoader}
import output.Link
import java.util.logging.Logger
import java.util.concurrent.{TimeUnit, Executors}
import collection.mutable.{ArrayBuffer, SynchronizedBuffer}
import java.io.File

object Silk
{
    def main(args : Array[String])
    {
        val configFile = System.getProperty("configFile") match
        {
            case fileName : String => new File(fileName)
            case _ => throw new IllegalArgumentException("No configuration file specified. Please set the 'configFile' property")
        }

        val silk = new Silk(configFile)
        silk.loadPartitions()
        silk.generateLinks()
    }
}

class Silk(configFile : File)
{
    private val logger = Logger.getLogger(classOf[Silk].getName)

    private val partitionCacheDir = new File("./partitionCache/")

    private val config = ConfigLoader.load(configFile)
    private val linkSpec = config.linkSpecs.values.head

    private val sourcePartitionCache : PartitionCache= new FilePartitionCache(new File(partitionCacheDir + "/" + linkSpec.sourceDatasetSpecification.dataSource.id + "/"))
    private val targetPartitionCache : PartitionCache= new FilePartitionCache(new File(partitionCacheDir + "/" + linkSpec.targetDatasetSpecification.dataSource.id + "/"))

    def loadPartitions()
    {
        logger.info("Loading partitions")

        //Create instance specifications
        val (sourceInstanceSpec, targetInstanceSpec) = InstanceSpecification.retrieve(linkSpec)
        println(sourceInstanceSpec)
        println(targetInstanceSpec)

        //Retrieve instances
        val sourceInstances = linkSpec.sourceDatasetSpecification.dataSource.retrieve(config, sourceInstanceSpec)
        val targetInstances = linkSpec.targetDatasetSpecification.dataSource.retrieve(config, targetInstanceSpec)

        sourcePartitionCache.write(sourceInstances)
        targetPartitionCache.write(targetInstances)
    }

    def generateLinks()
    {
        logger.info("Generating links")

        val startTime = System.currentTimeMillis()

        //Check if any partitions have been found
        if(sourcePartitionCache.size == 0 && targetPartitionCache.size == 0)
        {
            logger.warning("No partitions found in " + partitionCacheDir)
        }

        //Execute match tasks
        val executor = Executors.newFixedThreadPool(2)
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

                for(link <- bestLinks) linkSpec.outputs.foreach(_.write(link))
            }
        }
        else
        {
            for(link <- linkBuffer) linkSpec.outputs.foreach(_.write(link))
        }

        linkSpec.outputs.foreach(_.close)

        logger.info("Generated " + linkBuffer.size + " links in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
    }

    private class MatchTask(sourcePartitionIndex : Int, targetPartitionIndex : Int, callback : Link => Unit) extends Runnable
    {
        override def run() : Unit =
        {
            val taskNum = (sourcePartitionIndex * targetPartitionCache.size + targetPartitionIndex) + 1
            val taskCount = sourcePartitionCache.size * targetPartitionCache.size
            logger.info("Starting task " + taskNum + " of " + taskCount)

            val linkPredicate = config.resolvePrefix(linkSpec.linkType)

            for(sourceInstance <- sourcePartitionCache(sourcePartitionIndex);
                targetInstance <- targetPartitionCache(targetPartitionIndex))
            {
                val confidence = linkSpec.condition.evaluate(sourceInstance, targetInstance).headOption.getOrElse(0.0)

                if(confidence >= linkSpec.filter.threshold)
                {
                    callback(new Link(sourceInstance.uri, linkPredicate, targetInstance.uri, confidence))
                }
            }

            logger.info("Completed task " + taskNum + " of " + taskCount)
        }
    }
}
