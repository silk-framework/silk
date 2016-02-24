/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework

import java.io.{File, OutputStreamWriter}
import java.util.logging.{Level, Logger}

import org.apache.log4j.{ConsoleAppender, PatternLayout}
import org.silkframework.config.{Config, LinkSpecification, LinkingConfig, TransformSpecification}
import org.silkframework.execution.{ExecuteTransform, GenerateLinks}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.resource.FileResourceManager
import org.silkframework.runtime.serialization.Serialization
import org.silkframework.util.CollectLogs
import org.silkframework.util.StringUtils._

import scala.math.max
import scala.xml.XML

/**
 * Executes the complete Silk workflow.
 */
object Silk {

  // Initialize config
  Config()

  /**
   * The default number of threads to be used for matching.
   */
  val DefaultThreads = max(8, Runtime.getRuntime.availableProcessors())

  private val logger = Logger.getLogger(Silk.getClass.getName)

  //Print welcome message on start-up
  println("Silk Link Discovery Framework - Version 2.7.1")

  // Initialize Log4j
  val ca = new ConsoleAppender()
  ca.setWriter(new OutputStreamWriter(System.out))
  ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"))
  ca.setThreshold(org.apache.log4j.Level.WARN)
  org.apache.log4j.Logger.getRootLogger.addAppender(ca)

  /**
   * Executes Silk.
   * The execution is configured using the following properties:
   *  - 'configFile' (required): The configuration file
   *  - 'linkSpec' (optional): The link specifications to be executed. If not given, all link specifications are executed.
   *  - 'threads' (optional): The number of threads to be be used for matching.
   *  - 'reload' (optional): Specifies if the entity cache is to be reloaded before executing the matching. Default: true
   */
  def execute() {
    System.getProperty("logQueries") match {
      case BooleanLiteral(b) if b =>
        Logger.getLogger("org.silkframework.plugins.dataset.rdf").setLevel(Level.FINE)
        Logger.getLogger("").getHandlers.foreach(_.setLevel(Level.FINE))
      case _ =>
    }

    val configFile = System.getProperty("configFile") match {
      case fileName: String => new File(fileName)
      case _ => throw new IllegalArgumentException("No configuration file specified. Please set the 'configFile' property")
    }

    val linkSpec = System.getProperty("linkSpec")

    val numThreads = System.getProperty("threads") match {
      case IntLiteral(num) => num
      case str: String => throw new IllegalArgumentException("Property 'threads' must be an integer")
      case _ => DefaultThreads
    }

    val reload = System.getProperty("reload") match {
      case BooleanLiteral(b) => b
      case str: String => throw new IllegalArgumentException("Property 'reload' must be a boolean")
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
   * @param reload Specifies if the entity cache is to be reloaded before executing the matching. Default: true
   */
  def executeFile(configFile: File, linkSpecID: String = null, numThreads: Int = DefaultThreads, reload: Boolean = true) {
    implicit val resourceLoader = new FileResourceManager(configFile.getAbsoluteFile.getParentFile)
    val config = Serialization.fromXml[LinkingConfig](XML.loadFile(configFile))
    executeConfig(config, linkSpecID, numThreads, reload)
  }

  /**
   * Executes Silk using a specific configuration.
   *
   * @param config The configuration.
   * @param linkSpecID The link specifications to be executed. If not given, all link specifications are executed.
   * @param numThreads The number of threads to be used for matching.
   * @param reload Specifies if the entity cache is to be reloaded before executing the matching. Default: true
   */
  def executeConfig(config: LinkingConfig, linkSpecID: String = null, numThreads: Int = DefaultThreads, reload: Boolean = true) {

    if (linkSpecID != null) {

      logger.log(Level.INFO, s"Executing [ id :: $linkSpecID ].")

      // Execute a specific link specification
      config.interlink(linkSpecID).foreach(executeLinkSpec(config, _, numThreads, reload))

      // Execute each transform with the provided id.
      config.transform(linkSpecID).foreach(executeTransform(config, _))

    } else {

      logger.log(Level.INFO, s"Executing all.")

      //Execute all link specifications
      for (linkSpec <- config.linkSpecs) {
        executeLinkSpec(config, linkSpec, numThreads, reload)
      }

      // Execute all transforms.
      config.transforms.foreach(executeTransform(config, _))
    }
  }

  /**
   * Executes a single link specification.
   *
   * @param config The configuration.
   * @param linkSpec The link specifications to be executed.
   * @param numThreads The number of threads to be used for matching.
   * @param reload Specifies if the entity cache is to be reloaded before executing the matching. Default: true
   */
  private def executeLinkSpec(config: LinkingConfig, linkSpec: LinkSpecification, numThreads: Int = DefaultThreads, reload: Boolean = true): Unit = {
    val generateLinks =
      new GenerateLinks(
        inputs = linkSpec.findSources(config.sources),
        linkSpec = linkSpec,
        outputs = config.outputs.map(_.linkSink),
        runtimeConfig = config.runtime.copy(numThreads = numThreads, reloadCache = reload)
      )
    Activity(generateLinks).startBlocking()
  }

  /**
   * Execute a transform with the provided transform specification.
   *
   * @since 2.6.1
   *
   * @param transform The transform specification.
   */
  private def executeTransform(config: LinkingConfig, transform: TransformSpecification): Unit = {
    val input = config.source(transform.selection.datasetId).source
    Activity(new ExecuteTransform(input, transform.selection, transform.rules, config.outputs.map(_.entitySink))).startBlocking()
  }

  /**
   * Main method to allow Silk to be started from the command line.
   */
  def main(args: Array[String]) {
    val logs = CollectLogs() {
      execute()
    }

    if (logs.isEmpty) {
      logger.info("Finished execution successfully")
    } else {
      logger.warning("The following warnings haven been generated during the execution:\n- " + logs.map(_.getMessage).mkString("\n- "))
    }
  }
}
