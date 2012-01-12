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

package de.fuberlin.wiwiss.silk

import config.LinkingConfig
import plugins.Plugins
import java.io.File
import config.LinkSpecification
import plugins.jena.JenaPlugins
import util.StringUtils._
import util.CollectLogs
import java.util.logging.{Level, Logger}

/**
 * Executes the complete Silk workflow.
 */
object Silk {
  /**
   * The default number of threads to be used for matching.
   */
  val DefaultThreads = 8

  private val logger = Logger.getLogger(Silk.getClass.getName)

  //Print welcome message on start-up
  println("Silk Link Discovery Framework - Version 2.5.3")

  //Register all available plugins
  Plugins.register()
  JenaPlugins.register()

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
      case BooleanLiteral(b) if b => {
        Logger.getLogger("de.fuberlin.wiwiss.silk.util.sparql").setLevel(Level.FINE)
        Logger.getLogger("").getHandlers.foreach(_.setLevel(Level.FINE))
      }
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
    executeConfig(LinkingConfig.load(configFile), linkSpecID, numThreads, reload)
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
      //Execute a specific link specification
      val linkSpec = config.linkSpec(linkSpecID)
      executeLinkSpec(config, linkSpec, numThreads, reload)
    } else {
      //Execute all link specifications
      for (linkSpec <- config.linkSpecs) {
        executeLinkSpec(config, linkSpec, numThreads, reload)
      }
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
  private def executeLinkSpec(config: LinkingConfig, linkSpec: LinkSpecification, numThreads: Int = DefaultThreads, reload: Boolean = true) {
    new GenerateLinksTask(
      sources = config.sources,
      linkSpec = linkSpec,
      outputs = linkSpec.outputs ++ config.outputs,
      runtimeConfig = config.runtime.copy(numThreads = numThreads, reloadCache = reload)
    ).apply()
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
