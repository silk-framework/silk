package de.fuberlin.wiwiss.silk

import config.{Configuration, ConfigReader}
import impl.DefaultImplementations
import instance.FileInstanceCache
import jena.{FileDataSource, RdfDataSource}
import linkspec.LinkSpecification
import datasource.DataSource
import java.io.File
import java.util.logging.Logger
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
  val DefaultThreads = 8

  /**
   * The directory the instance cache will be written to
   */
  private val instanceCacheDir = new File("./instanceCache/")

  DefaultImplementations.register()
  DataSource.register("rdf", classOf[RdfDataSource])
  DataSource.register("file", classOf[FileDataSource])

  /**
   * Executes Silk.
   * The execution is configured using the following properties:
   *  - 'configFile' (required): The configuration file
   *  - 'linkSpec' (optional): The link specifications to be executed. If not given, all link specifications are executed.
   *  - 'threads' (optional): The number of threads to be be used for matching.
   *  - 'reload' (optional): Specifies if the instance cache is to be reloaded before executing the matching. Default: true
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

    val reload = System.getProperty("reload") match
    {
      case BooleanLiteral(b) => b
      case str : String => throw new IllegalArgumentException("Property 'reload' must be an boolean")
      case _ => true
    }

    executeFile(configFile, linkSpec, numThreads, reload)
  }

  /**
   * Executes Silk using a specific configuration file.
   *
   * @param configFile The configuration file.
   * @param linkSpecID The link specifications to be executed. If not given, all link specifications are executed.
   * @param numThreads The number of threads to be used for matching.
   * @param reload Specifies if the instance cache is to be reloaded before executing the matching. Default: true
   */
  def executeFile(configFile : File, linkSpecID : String = null, numThreads : Int = DefaultThreads, reload : Boolean = true)
  {
    executeConfig(ConfigReader.read(configFile), linkSpecID, numThreads, reload)
  }

  /**
   * Executes Silk using a specific configuration.
   *
   * @param configFile The configuration.
   * @param linkSpecID The link specifications to be executed. If not given, all link specifications are executed.
   * @param numThreads The number of threads to be used for matching.
   * @param reload Specifies if the instance cache is to be reloaded before executing the matching. Default: true
   */
  def executeConfig(config : Configuration, linkSpecID : String = null, numThreads : Int = DefaultThreads, reload : Boolean = true)
  {
    if(linkSpecID != null)
    {
      val linkSpec = config.linkSpec(linkSpecID) match
      {
        case Some(ls) => ls
        case None => throw new IllegalArgumentException("Unknown link specification: " + linkSpecID)
      }

      executeLinkSpec(config, linkSpec, numThreads, reload)
    }
    else
    {
      for(linkSpec <- config.linkSpecs)
      {
        executeLinkSpec(config, linkSpec, numThreads, reload)
      }
    }
  }

  private def executeLinkSpec(config : Configuration, linkSpec : LinkSpecification, numThreads : Int = DefaultThreads, reload : Boolean = true)
  {
    val startTime = System.currentTimeMillis()
    logger.info("Silk started")

    //Create instance caches
    val numBlocks = 1//linkSpec.condition.blockCount
    val sourceCache = new FileInstanceCache(new File(instanceCacheDir + "/source/" + linkSpec.id + "/"), numBlocks)
    val targetCache = new FileInstanceCache(new File(instanceCacheDir + "/target/" + linkSpec.id + "/"), numBlocks)

    //Load instances into cache
    if(reload)
    {
      val loadSourceCacheTask = new LoadTask(config, linkSpec, Some(sourceCache), None)
      val loadTargetCacheTask = new LoadTask(config, linkSpec, None, Some(targetCache))

      loadSourceCacheTask.runInBackground()
      loadTargetCacheTask.runInBackground()

      //Wait until caches are being written
      while((loadSourceCacheTask.isRunning && !sourceCache.isWriting) || (loadTargetCacheTask.isRunning && !targetCache.isWriting))
      {
        Thread.sleep(100)
      }
    }

    //Execute matching
    val matchTask = new MatchTask(config, linkSpec, sourceCache, targetCache, numThreads)
    val links = matchTask()

    //Filter links
    val filterTask = new FilterTask(linkSpec, links)
    val filteredLinks = filterTask()

    //Write links
    val outputTask = new OutputTask(config, linkSpec, filteredLinks)
    outputTask()

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
