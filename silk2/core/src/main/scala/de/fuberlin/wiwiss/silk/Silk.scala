package de.fuberlin.wiwiss.silk

import config.{Configuration, ConfigLoader}
import impl.DefaultImplementations
import instance.{FileInstanceCache, InstanceCache, InstanceSpecification}
import linkspec.LinkSpecification
import output.Link
import java.util.concurrent.{TimeUnit, Executors}
import java.io.File
import collection.mutable.{Buffer, ArrayBuffer, SynchronizedBuffer}
import java.util.logging.{Level, Logger}
import util.StringUtils._

/**
 * Executes the complete Silk workflow.
 */
object Silk
{
    private val logger = Logger.getLogger(Silk.getClass.getName)

    /**
     * The default number of threads to be used for matching.
     */
    val DefaultThreads = 4

    /**
     * The directory the instance cache will be written to
     */
    private val instanceCacheDir = new File("./instanceCache/")
    
    DefaultImplementations.register()

    /**
     *  Executes Silk.
     * The execution is configured using the following properties:
     *  - 'configFile' (required): The configuration file
     *  - 'linkSpec' (optional): The link specifications to be executed. If not given, all link specifications are executed.
     *  - 'threads' (optional): The number of threads to be be used for matching.
     */
    def execute()
    {
        val configFile = System.getProperty("configFile") match
        {
            case fileName : String => new File(fileName)
            case _ => throw new IllegalArgumentException("No configuration file specified. Please set the 'configFile' property")
        }

        val linkSpec = System.getProperty("linkSpec")

        val numThreads = System.getProperty("threads") match
        {
            case IntLiteral(num) => num
            case str : String => throw new IllegalArgumentException("Property 'threads' must be an integer")
            case _ => DefaultThreads
        }

        executeFile(configFile, linkSpec, numThreads)
    }

    /**
     * Executes Silk using a specific configuration file.
     *
     * @param configFile The configuration file.
     * @param linkSpecID The link specifications to be executed. If not given, all link specifications are executed.
     * @param numThreads The number of threads to be used for matching.
     */
    def executeFile(configFile : File, linkSpecID : String = null, numThreads : Int = DefaultThreads)
    {
        executeConfig(ConfigLoader.load(configFile), linkSpecID, numThreads)
    }

    /**
     * Executes Silk using a specific configuration.
     *
     * @param configFile The configuration.
     * @param linkSpecID The link specifications to be executed. If not given, all link specifications are executed.
     * @param numThreads The number of threads to be used for matching.
     */
    def executeConfig(config : Configuration, linkSpecID : String = null, numThreads : Int = DefaultThreads)
    {
        if(linkSpecID != null)
        {
             val linkSpec = config.linkSpecs.get(linkSpecID) match
             {
                 case Some(ls) => ls
                 case None => throw new IllegalArgumentException("Unknown link specification: " + linkSpecID)
             }

             executeLinkSpec(config, linkSpec, numThreads)
        }
        else
        {
            for(linkSpec <- config.linkSpecs.values)
            {
                executeLinkSpec(config, linkSpec, numThreads)
            }
        }
    }

    private def executeLinkSpec(config : Configuration, linkSpec : LinkSpecification, numThreads : Int = DefaultThreads)
    {
        val startTime = System.currentTimeMillis()
        logger.info("Silk started")

        //Create instance caches
        val numBlocks = linkSpec.blocking.map(_.blocks).getOrElse(1)
        val sourceCache = new FileInstanceCache(new File(instanceCacheDir + "/source/"), numBlocks)
        val targetCache = new FileInstanceCache(new File(instanceCacheDir + "/target/"), numBlocks)

        //Load instances
        val loader = new Loader(config, linkSpec)
        loader.loadCaches(sourceCache, targetCache)

        //Execute matching
        val matcher = new Matcher(config, linkSpec, numThreads)
        matcher.execute(sourceCache, targetCache)

        logger.info("Total time: " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
    }

    /**
     * Main method to allow Silk to be started from the command line.
     */
    def main(args : Array[String])
    {
        execute()
    }
}

/**
 * Loads the instance cache
 */
class Loader(config : Configuration, linkSpec : LinkSpecification)
{
    private val (sourceInstanceSpec, targetInstanceSpec) = InstanceSpecification.retrieve(linkSpec)

    private val logger = Logger.getLogger(classOf[Loader].getName)

    def loadCaches(sourceCache : InstanceCache, targetCache : InstanceCache)
    {
        val startTime = System.currentTimeMillis()
        logger.info("Loading instances")

        loadSourceCache(sourceCache)
        loadTargetCache(targetCache)

        logger.info("Loaded instances in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
    }

    def loadSourceCache(sourceCache : InstanceCache)
    {
        val sourceInstances = linkSpec.sourceDatasetSpecification.dataSource.retrieve(sourceInstanceSpec, config.prefixes)

        logger.info("Loading instances of source dataset")
        linkSpec.blocking match
        {
            case Some(blocking) => sourceCache.write(sourceInstances, blocking)
            case None => sourceCache.write(sourceInstances)
        }
    }

    def loadTargetCache(targetCache : InstanceCache)
    {
        val targetInstances = linkSpec.targetDatasetSpecification.dataSource.retrieve(targetInstanceSpec, config.prefixes)

        logger.info("Loading instances of target dataset")
        linkSpec.blocking match
        {
            case Some(blocking) => targetCache.write(targetInstances, blocking)
            case None => targetCache.write(targetInstances)
        }
    }
}

/**
 * Executes the matching.
 */
class Matcher(config : Configuration, linkSpec : LinkSpecification, numThreads : Int = Silk.DefaultThreads)
{
    private val logger = Logger.getLogger(classOf[Matcher].getName)

    /**
     * Executes the matching.
     */
    def execute(sourceCache : InstanceCache, targetCache : InstanceCache)
    {
        require(sourceCache.blockCount == targetCache.blockCount, "sourceCache.blockCount == targetCache.blockCount")

        val startTime = System.currentTimeMillis()
        logger.info("Starting matching")

        var links = generateLinks(sourceCache, targetCache)
        links = filterLinks(links)
        writeOutput(links)

        logger.info("Executed matching in " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
    }

    /**
     * Generates links between the instances according to the link specification.
     */
    private def generateLinks(sourceCache : InstanceCache, targetCache : InstanceCache) : Buffer[Link] =
    {
        val executor = Executors.newFixedThreadPool(numThreads)
        val linkBuffer = new ArrayBuffer[Link]() with SynchronizedBuffer[Link]

        for(blockIndex <- 0 until sourceCache.blockCount;
            sourcePartitionIndex <- 0 until sourceCache.partitionCount(blockIndex);
            targetPartitionIndex <- 0 until targetCache.partitionCount(blockIndex))
        {
            executor.submit(new MatchTask(sourceCache, targetCache, blockIndex, sourcePartitionIndex, targetPartitionIndex, link => linkBuffer.append(link)))
        }

        executor.shutdown()
        executor.awaitTermination(1000, TimeUnit.DAYS)

        linkBuffer
    }

    /**
     * Filters the links according to the link limit.
     */
    private def filterLinks(links : Buffer[Link]) : Buffer[Link] =
    {
        linkSpec.filter.limit match
        {
            case Some(limit) =>
            {
                val linkBuffer = new ArrayBuffer[Link]()
                logger.info("Filtering output")

                for((sourceUri, groupedLinks) <- links.groupBy(_.sourceUri))
                {
                    val bestLinks = groupedLinks.sortWith(_.confidence > _.confidence).take(limit)

                    linkBuffer.appendAll(bestLinks)
                }

                linkBuffer
            }
            case None => links
        }
    }

    /**
     * Writes the links to the output.
     */
    private def writeOutput(linkBuffer : Buffer[Link]) =
    {
        val outputs = config.outputs ++ linkSpec.outputs

        outputs.foreach(_.open)

        for(link <- linkBuffer;
            output <- outputs)
        {
            output.write(link, linkSpec.linkType)
        }

        outputs.foreach(_.close)
    }

    /**
     * A match task, which matches the instances of a single partition.
     */
    private class MatchTask(sourceCache : InstanceCache, targetCache : InstanceCache, blockIndex : Int,
        sourcePartitionIndex : Int, targetPartitionIndex : Int, callback : Link => Unit) extends Runnable
    {
        override def run() : Unit =
        {
            try
            {
                val tasksPerBlock = for(block <- 0 until sourceCache.blockCount) yield sourceCache.partitionCount(block) * targetCache.partitionCount(block)
                val taskNum = tasksPerBlock.take(blockIndex).foldLeft(sourcePartitionIndex * targetCache.partitionCount(blockIndex) + targetPartitionIndex + 1)(_ + _)
                val taskCount = tasksPerBlock.reduceLeft(_ + _)

                logger.info("Starting match task " + taskNum + " of " + taskCount)

                for(sourceInstance <- sourceCache.read(blockIndex, sourcePartitionIndex);
                    targetInstance <- targetCache.read(blockIndex, targetPartitionIndex))
                {
                    val confidence = linkSpec.condition(sourceInstance, targetInstance)

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
