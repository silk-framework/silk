package de.fuberlin.wiwiss.silk

import config.SilkConfig
import impl.DefaultImplementations
import instance.{Instance, InstanceSpecification, FileInstanceCache}
import jena.{FileDataSource, RdfDataSource}
import datasource.DataSource
import java.io.File
import linkspec.{LinkSpecification}
import util.StringUtils._
import util.{CollectLogs, Future, SourceTargetPair}
import java.util.logging.{Level, Logger}

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
  private val instanceCacheDir = new File(System.getProperty("user.home") + "/.silk/instanceCache/")

  DefaultImplementations.register()
  DataSource.register(classOf[RdfDataSource])
  DataSource.register(classOf[FileDataSource])

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
    System.getProperty("logQueries") match
    {
      case BooleanLiteral(b) if b =>
      {
        Logger.getLogger("de.fuberlin.wiwiss.silk.util.sparql").setLevel(Level.FINE)
        Logger.getLogger("").getHandlers.foreach(_.setLevel(Level.FINE))
      }
      case _ =>
    }

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
      case str : String => throw new IllegalArgumentException("Property 'reload' must be a boolean")
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
    executeConfig(SilkConfig.load(configFile), linkSpecID, numThreads, reload)
  }

  /**
   * Executes Silk using a specific configuration.
   *
   * @param configFile The configuration.
   * @param linkSpecID The link specifications to be executed. If not given, all link specifications are executed.
   * @param numThreads The number of threads to be used for matching.
   * @param reload Specifies if the instance cache is to be reloaded before executing the matching. Default: true
   */
  def executeConfig(config : SilkConfig, linkSpecID : String = null, numThreads : Int = DefaultThreads, reload : Boolean = true)
  {
    if(linkSpecID != null)
    {
      val linkSpec = config.linkSpec(linkSpecID)

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

  private def executeLinkSpec(config : SilkConfig, linkSpec : LinkSpecification, numThreads : Int = DefaultThreads, reload : Boolean = true)
  {
    val startTime = System.currentTimeMillis()

    //Retrieve Instance Specifications from Link Specification
    val instanceSpecs = InstanceSpecification.retrieve(linkSpec)

    //Create instance caches
    val caches = SourceTargetPair(
        new FileInstanceCache(instanceSpecs.source, new File(instanceCacheDir + "/" + linkSpec.id + "/source/"), reload, config.blocking.map(_.blocks).getOrElse(1)),
        new FileInstanceCache(instanceSpecs.target, new File(instanceCacheDir + "/" + linkSpec.id + "/target/"), reload, config.blocking.map(_.blocks).getOrElse(1))
      )

    //Load instances into cache
    if(reload)
    {
      val sources = linkSpec.datasets.map(_.sourceId).map(config.source(_))

      def blockingFunction(instance : Instance) = linkSpec.condition.index(instance).map(_ % config.blocking.map(_.blocks).getOrElse(1))

      val loadTask = new LoadTask(sources, caches, instanceSpecs, if(config.blocking.isDefined) Some(blockingFunction _) else None)
      loadTask.runInBackground()
    }

    //Execute matching
    val matchTask = new MatchTask(linkSpec, caches, numThreads)
    val links = matchTask()

    //Filter links
    val filterTask = new FilterTask(links, linkSpec.filter)
    val filteredLinks = filterTask()

    //Write links
    val outputTask = new OutputTask(filteredLinks, linkSpec.linkType, config.outputs ++ linkSpec.outputs)
    outputTask()

    logger.info("Total time: " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
  }

  /**
   * Main method to allow Silk to be started from the command line.
   */
  def main(args : Array[String])
  {
    val logs = CollectLogs()
    {
      execute()
    }

    if(logs.isEmpty)
    {
      logger.info("Finished execution successfully")
    }
    else
    {
      logger.warning("The following warnings haven been generated during the execution:\n- " + logs.map(_.getMessage).mkString("\n- "))
    }
  }
}
