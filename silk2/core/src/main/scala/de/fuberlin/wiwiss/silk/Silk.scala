package de.fuberlin.wiwiss.silk

import config.{Configuration, ConfigLoader}
import datasource._
import linkspec.LinkSpecification
import output.Link
import java.util.concurrent.{TimeUnit, Executors}
import collection.mutable.{ArrayBuffer, SynchronizedBuffer}
import java.io.File
import java.util.logging.{Level, Logger}

object Silk
{
    private val logger = Logger.getLogger(Silk.getClass.getName)

    def main(args : Array[String])
    {
        val startTime = System.currentTimeMillis()
        logger.info("Silk started")

        val configFile = System.getProperty("configFile") match
        {
            case fileName : String => new File(fileName)
            case _ => throw new IllegalArgumentException("No configuration file specified. Please set the 'configFile' property")
        }

        val config = ConfigLoader.load(configFile)
        val linkSpec = config.linkSpecs.values.head

        val silk = new Silk(config, linkSpec)
        silk.load()
        silk.generateLinks()

        logger.info("Total time: " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
    }
}

class Silk(config : Configuration, linkSpec : LinkSpecification)
{
    private val logger = Logger.getLogger(classOf[Silk].getName)

    private val partitionCacheDir = new File("./partitionCache/")

    private val numBlocks = 1

    private val sourceCache : InstanceCache = new FileInstanceCache(new File(partitionCacheDir + "/source/"), numBlocks)
    private val targetCache : InstanceCache = new FileInstanceCache(new File(partitionCacheDir + "/target/"), numBlocks)

    def load()
    {
        val startTime = System.currentTimeMillis()
        logger.info("Loading instances")

        //Create instance specifications
        val (sourceInstanceSpec, targetInstanceSpec) = InstanceSpecification.retrieve(linkSpec)
        println(sourceInstanceSpec)
        println(targetInstanceSpec)

        //Retrieve instances
        val sourceInstances = linkSpec.sourceDatasetSpecification.dataSource.retrieve(sourceInstanceSpec, config.prefixes)
        val targetInstances = linkSpec.targetDatasetSpecification.dataSource.retrieve(targetInstanceSpec, config.prefixes)

        logger.info("Loading instances of source dataset")
        sourceCache.write(sourceInstances)
        
        logger.info("Loading instances of target dataset")
        targetCache.write(targetInstances)

        logger.info("Loaded instances in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
    }

    def generateLinks()
    {
        val startTime = System.currentTimeMillis()
        logger.info("Generating links")

        //Execute match tasks
        val executor = Executors.newFixedThreadPool(4)
        val linkBuffer = new ArrayBuffer[Link]() with SynchronizedBuffer[Link]

        for(blockIndex <- 0 until numBlocks;
            sourcePartitionIndex <- 0 until sourceCache.partitionCount(blockIndex);
            targetPartitionIndex <- 0 until targetCache.partitionCount(blockIndex))
        {
            executor.submit(new MatchTask(blockIndex, sourcePartitionIndex, targetPartitionIndex, link => linkBuffer.append(link)))
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

    private class MatchTask(blockIndex : Int, sourcePartitionIndex : Int, targetPartitionIndex : Int, callback : Link => Unit) extends Runnable
    {
        override def run() : Unit =
        {
            try
            {
                val tasksPerBlock = for(block <- 0 until numBlocks) yield sourceCache.partitionCount(block) * targetCache.partitionCount(block)
                val taskNum = tasksPerBlock.take(blockIndex).foldLeft(sourcePartitionIndex * targetCache.partitionCount(blockIndex) + targetPartitionIndex + 1)(_ + _)
                val taskCount = tasksPerBlock.reduceLeft(_ + _)

                logger.info("Starting task " + taskNum + " of " + taskCount)

                for(sourceInstance <- sourceCache.read(blockIndex, sourcePartitionIndex);
                    targetInstance <- targetCache.read(blockIndex, targetPartitionIndex))
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
