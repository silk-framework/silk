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

import java.io.File
import java.util.logging.{Level, Logger}

import javax.inject.Inject
import org.silkframework.config._
import org.silkframework.dataset.CombinedEntitySink
import org.silkframework.rule.execution.{ExecuteTransform, GenerateLinks}
import org.silkframework.rule.{LinkSpec, LinkingConfig, TransformSpec}
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.runtime.resource.FileResourceManager
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.util.StringUtils._
import org.silkframework.util.{CollectLogs, Identifier}
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutor, Workflow}
import org.silkframework.workspace.resources.FileRepository
import org.silkframework.workspace.{InMemoryWorkspaceProvider, Project, ProjectMarshallerRegistry, Workspace}

import scala.math.max
import scala.xml.XML

/**
 * Executes the complete Silk workflow.
 */
object Silk {
  @Inject
  private var configMgr: Config = DefaultConfig.instance
  configMgr()

  implicit val userContext: UserContext = UserContext.Empty // No user context in single machine mode

  /**
   * The default number of threads to be used for matching.
   */
  val DefaultThreads: Int = max(8, Runtime.getRuntime.availableProcessors())

  private val logger = Logger.getLogger(Silk.getClass.getName)

  // Initialize Log4j
//  val ca = new ConsoleAppender()
//  ca.setWriter(new OutputStreamWriter(System.out))
//  ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"))
//  ca.setThreshold(org.apache.log4j.Level.WARN)
//  org.apache.log4j.Logger.getRootLogger.addAppender(ca)

  /**
   * Executes Silk.
   * The execution is configured using the following properties:
   *  - 'configFile' (required): The configuration file
   *  - 'task' (optional): The task (link specification or workflow) to be executed.
   *  - 'threads' (optional): The number of threads to be be used for matching.
   *  - 'reload' (optional): Specifies if the entity cache is to be reloaded before executing the matching. Default: true
   */
  def execute(): Unit = {
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

    var task = System.getProperty("task")
    if(task == null)
      task = System.getProperty("linkSpec") // Legacy parameter

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

    if(configFile.getName.endsWith(".xml")) {
      executeFile(configFile, task, numThreads, reload)
    } else {
      if(task == null)
        throw new IllegalArgumentException("The given config file appears to be a project, but no task name has been specified using the 'task' property.")
      executeProject(configFile, task)
    }
  }

  /**
   * Executes Silk using a specific configuration file.
   *
   * @param configFile The configuration file.
   * @param linkSpecID The link specifications to be executed. If not given, all link specifications are executed.
   * @param numThreads The number of threads to be used for matching.
   * @param reload Specifies if the entity cache is to be reloaded before executing the matching. Default: true
   */
  def executeFile(configFile: File, linkSpecID: String = null, numThreads: Int = DefaultThreads, reload: Boolean = true)
                 (implicit userContext: UserContext): Unit = {
    implicit val readContext: ReadContext = ReadContext(new FileResourceManager(configFile.getAbsoluteFile.getParentFile))
    val config = XmlSerialization.fromXml[LinkingConfig](XML.loadFile(configFile))
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
  def executeConfig(config: LinkingConfig, linkSpecID: String = null, numThreads: Int = DefaultThreads, reload: Boolean = true)
                   (implicit userContext: UserContext): Unit = {

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
  private def executeLinkSpec(config: LinkingConfig, linkSpec: Task[LinkSpec], numThreads: Int = DefaultThreads, reload: Boolean = true)
                             (implicit userContext: UserContext): Unit = {
    val generateLinks =
      new GenerateLinks(
        id = linkSpec.id,
        label = linkSpec.id,
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
   * @param transform The transform specification.
   */
  private def executeTransform(config: LinkingConfig, transform: Task[TransformSpec]): Unit = {
    val input = config.source(transform.selection.inputId).source
    Activity(new ExecuteTransform(transform.taskLabel(), (_) => input, transform.data, (_) => new CombinedEntitySink(config.outputs.map(_.entitySink)))).startBlocking() // TODO: Allow to set error output
  }

  /**
    * Executes a Silk project.
    *
    * @param projectFile The project file
    * @param taskName The name of task in the project that should be executed. Currently only workflows are supported.
    */
  def executeProject(projectFile: File, taskName: Identifier): Project = {
    // Create workspace provider
    val projectId = Identifier("project")
    val workspaceProvider = new InMemoryWorkspaceProvider()
    val resourceRepository = FileRepository(".")

    // Import project
    val marshaller = ProjectMarshallerRegistry.marshallerForFile(projectFile.getName)
    marshaller.unmarshalProject(projectId, workspaceProvider, resourceRepository.get(projectId), projectFile)

    // Create a workspace from the import and get task
    val workspace = new Workspace(workspaceProvider, resourceRepository)
    val project = workspace.project(projectId)
    val task = project.task[Workflow](taskName)

    // Execute task
    val executor = LocalWorkflowExecutor(task)
    Activity(executor).startBlocking()

    project
  }

  /**
   * Main method to allow Silk to be started from the command line.
   */
  def main(args: Array[String]) {
    configMgr()
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
