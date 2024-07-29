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

package org.silkframework.workspace

import org.silkframework.config.{DefaultConfig, Prefixes}
import org.silkframework.runtime.activity.{HasValue, UserContext}
import org.silkframework.runtime.metrics.MeterRegistryProvider
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}
import org.silkframework.runtime.validation.{NotFoundException, ServiceUnavailableException}
import org.silkframework.util.Identifier
import org.silkframework.workspace.TaskCleanupPlugin.CleanUpAfterTaskDeletionFunction
import org.silkframework.workspace.activity.{GlobalWorkspaceActivity, GlobalWorkspaceActivityFactory}
import org.silkframework.workspace.exceptions.{IdentifierAlreadyExistsException, ProjectNotFoundException}
import org.silkframework.workspace.metrics.WorkspaceMetrics
import org.silkframework.workspace.resources.ResourceRepository

import java.io._
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.{Level, Logger}
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.control.NonFatal

/**
  * The workspace that manages loading of and access to workspace projects.
  *
  * @param provider    the workspace provider
  * @param repository  the resource repository
  */
class Workspace(val provider: WorkspaceProvider,
                val repository: ResourceRepository,
                val meterRegistryProvider: MeterRegistryProvider = MeterRegistryProvider())
  extends WorkspaceReadTrait {
  private val log = Logger.getLogger(classOf[Workspace].getName)

  private val cfg = DefaultConfig.instance()
  // Time in milliseconds to wait for the workspace to be loaded
  private val waitForWorkspaceInitialization = cfg.getLong("workspace.timeouts.waitForWorkspaceInitialization")
  private val loadProjectsLock = new ReentrantLock()

  lazy val cleanUpAfterTaskDeletion: CleanUpAfterTaskDeletionFunction = {
    TaskCleanupPlugin.retrieveCleanUpAfterTaskDeletionFunction
  }

  @volatile
  private var initialized = false

  @volatile
  private var cachedProjects: Seq[Project] = Seq.empty

  @volatile
  // Additional prefixes loaded from the workspace provider that will be added to every project
  private var additionalPrefixes: Prefixes = Prefixes.empty

  // All global workspace activities
  lazy val activities: Seq[GlobalWorkspaceActivity[_ <: HasValue]] = {
    val factories = PluginRegistry.availablePlugins[GlobalWorkspaceActivityFactory[_ <: HasValue]].toList
    var activities = List[GlobalWorkspaceActivity[_ <: HasValue]]()
    for(factory <- factories) {
      try {
        activities ::= new GlobalWorkspaceActivity(factory()(PluginContext.empty))
      } catch {
        case NonFatal(ex) =>
          val errorMsg = s"Could not load workspace activity '$factory'."
          log.log(Level.WARNING, errorMsg, ex)
      }
    }
    activities.reverse
  }

  /** Get global workspace activity by name. */
  def activity(activityName: String): GlobalWorkspaceActivity[_ <: HasValue] = {
    activities.find(_.name.toString == activityName)
        .getOrElse(throw NotFoundException(s"The workspace does not contain an activity named '$activityName'. " +
            s"Available activities: ${activities.map(_.name).mkString(", ")}"))
  }

  private lazy val activityMap: Map[Class[_], GlobalWorkspaceActivity[_ <: HasValue]] = activities.map(a => (a.factory.activityType, a)).toMap

  /** Get global workspace activity by type. */
  def activity[T <: HasValue : ClassTag]: GlobalWorkspaceActivity[T] = {
    val requestedClass = implicitly[ClassTag[T]].runtimeClass
    val activity = activityMap.getOrElse(requestedClass,
      throw new NoSuchElementException(s"The workspace does not contain an activity of type '${requestedClass.getName}'. " +
          s"Available activities:\n${activityMap.keys.map(_.getName).mkString("\n ")}"))
    activity.asInstanceOf[GlobalWorkspaceActivity[T]]
  }

  /** Starts all auto-run workspace activities. */
  private def startWorkspaceActivities()(implicit userContext: UserContext): Unit = {
    for(activity <- activities if activity.autoRun) {
      activity.control.start()
    }
  }

  /** Load the projects of a user into the workspace. At the moment all users have access to all projects, so this is only,
    * executed once. */
  private def loadUserProjects()(implicit userContext: UserContext): Unit = {
    // FIXME: Extension for access control should happen here.
    if (!initialized) { // Avoid lock
      if(loadProjectsLock.tryLock(waitForWorkspaceInitialization, TimeUnit.MILLISECONDS)) {
        try {
          if (!initialized) { // Should have changed by now, but loadProjects() could also have failed, so double-check
            loadProjects()
            initialized = true
            log.info("Register workspace metrics")
            registerWorkspaceMetrics(userContext)
          }
        } finally {
          loadProjectsLock.unlock()
        }
      } else {
        // Timeout
        if(!initialized) {
          throw ServiceUnavailableException("The DataIntegration workspace is currently being initialized. The request has timed out. Please try again later.")
        }
      }
    }
  }

  override def projects(implicit userContext: UserContext): Seq[Project] = {
    loadUserProjects()
    cachedProjects
  }

  /**
   * Retrieves a project by name.
   *
   * @throws java.util.NoSuchElementException If no project with the given name has been found
   */
  override def project(name: Identifier)(implicit userContext: UserContext): Project = {
    loadUserProjects()
    findProject(name).getOrElse(throw ProjectNotFoundException(name))
  }

  override def findProject(name: Identifier)(implicit userContext: UserContext): Option[Project] = {
    loadUserProjects()
    projects.find(_.id == name)
  }

  def createProject(config: ProjectConfig)
                   (implicit userContext: UserContext): Project = synchronized {
    val creationConfig = config.withMetaData(config.metaData.asNewMetaData)
    loadUserProjects()
    if(cachedProjects.exists(_.id == creationConfig.id)) {
      throw IdentifierAlreadyExistsException("Project " + creationConfig.id + " does already exist!")
    }
    provider.putProject(creationConfig)
    val newProject = new Project(creationConfig, provider, repository.get(creationConfig.id))
    addProjectToCache(newProject)
    newProject.setAdditionalPrefixes(additionalPrefixes)
    log.info(s"Created new project '${creationConfig.id}'. " + userContext.logInfo)
    newProject
  }

  def removeProject(name: Identifier)
                   (implicit userContext: UserContext): Unit = synchronized {
    loadUserProjects()
    // Cancel all project and task activities
    project(name).activities.foreach(_.control.cancel())
    val projectTasks = project(name).allTasks
    for(task <- projectTasks;
        activity <- task.activities) {
      activity.control.cancel()
    }
    provider.deleteProject(name)
    repository.removeProjectResources(name)
    provider.removeExternalTaskLoadingErrors(name)
    removeProjectFromCache(name)
    for(task <- projectTasks) {
      cleanUpAfterTaskDeletion(name, task.id, task.data)
    }
    log.info(s"Removed project '$name'. " + userContext.logInfo)
  }

  private def removeProjectFromCache(name: Identifier): Unit = {
    cachedProjects = cachedProjects.filterNot(_.id == name)
  }

  /**
    * Generic export method that marshals the project as implemented in the given [[ProjectMarshallingTrait]] object.
    *
    * @param name project name
    * @param outputStream the output stream to write the exported project to.
    * @param marshaller object that defines how the project should be marshaled.
    * @return
    */
  def exportProject(name: Identifier, outputStream: OutputStream, marshaller: ProjectMarshallingTrait)
                   (implicit userContext: UserContext): String = {
    loadUserProjects()
    marshaller.marshalProject(project(name), outputStream, repository.get(name))
  }

  /**
    * Generic project import method that unmarshals the project as implemented in the given [[ProjectMarshallingTrait]] object.
    *
    * @param name       project name
    * @param file       the file to read the project to import from
    * @param marshaller object that defines how the project should be unmarshaled.
    * @param overwrite  If true, then it will overwrite an existing project, else an exception is thrown.
    */
  def importProject(name: Identifier,
                    file: File,
                    marshaller: ProjectMarshallingTrait,
                    overwrite: Boolean = false)
                   (implicit userContext: UserContext) {
    loadUserProjects()
    synchronized {
      findProject(name) match {
        case Some(_) if !overwrite =>
          throw IdentifierAlreadyExistsException("Project " + name.toString + " does already exist!")
        case Some(_) =>
          removeProject(name)
        case None =>
      }
    }
    log.info(s"Starting import of project '$name'...")
    val start = System.currentTimeMillis()
    marshaller.unmarshalProject(name, provider, repository.get(name), file)
    reloadProjectInternal(name)
    log.info(s"Imported project '$name' in ${(System.currentTimeMillis() - start).toDouble / 1000}s. " + userContext.logInfo)
  }

  /**
    * Reloads this workspace.
    */
  def reload()(implicit userContext: UserContext): Unit = synchronized {
    loadUserProjects()

    // Stop all activities
    for(project <- projects) { // Should not work directly on the cached projects
      project.cancelActivities()
    }
    for(workspaceActivity <- activities) {
      workspaceActivity.control.cancel()
    }
    // Refresh workspace provider
    provider.refresh(repository)

    // Reload projects
    loadProjects()
  }

  def reloadProject(projectId: Identifier)
                   (implicit userContext: UserContext): Unit = synchronized {
    loadUserProjects()
    log.info(s"Reloading project with ID '$projectId' from backend.")
    reloadProjectInternal(projectId, throwError = true)
  }

  /** Reloads the registered prefixes if the workspace provider supports this operation. */
  def reloadPrefixes()(implicit userContext: UserContext): Unit = {
    additionalPrefixes = provider.fetchRegisteredPrefixes()
    cachedProjects foreach { project =>
      project.setAdditionalPrefixes(additionalPrefixes)
    }
  }

  /** Reload a project from the backend */
  private def reloadProjectInternal(id: Identifier, throwError: Boolean = false)
                                   (implicit userContext: UserContext): Unit = synchronized {
    // remove project
    Try(project(id).cancelActivities())
    removeProjectFromCache(id)
    provider.readProject(id) match {
      case Some(projectConfig) =>
        val project = new Project(projectConfig, provider, repository.get(projectConfig.id))
        addProjectToCache(project)
        project.startActivities()
      case None =>
        val errorMessage = s"Project '$id' could not be reloaded in workspace, because it could not be read from the workspace provider!"
        log.warning(errorMessage)
        if(throwError) {
          throw new RuntimeException(errorMessage)
        }
    }
  }

  private def addProjectToCache(project: Project): Unit = {
    cachedProjects :+= project
  }

  /**
    * Removes all projects from this workspace.
    */
  def clear()(implicit userContext: UserContext): Unit = {
    loadUserProjects()
    for(project <- projects) {
      removeProject(project.config.id) // FIXME: This works directly on the cached projects and not the ones the user can see. Will be fixed in CMEM-998
    }
  }

  private def loadProjects()(implicit userContext: UserContext): Unit = {
    cachedProjects = for(projectConfig <- provider.readProjects()) yield {
      log.info("Loading project: " + projectConfig.id)
      val project = new Project(projectConfig, provider, repository.get(projectConfig.id))
      log.info("Finished loading project '" + projectConfig.id + "'.")
      project
    }
    log.info("Loading registered prefixes...")
    reloadPrefixes()
    log.info("Starting workspace activities...")
    startWorkspaceActivities()
    log.info("Starting project activities...")
    for(project <- cachedProjects) {
      project.startActivities()
    }
    log.info(s"${cachedProjects.size} projects loaded.")
  }

  private def registerWorkspaceMetrics(implicit userContext: UserContext): Unit =
    new WorkspaceMetrics(() => projects, () => projects.flatMap(_.allTasks)).bindTo(meterRegistryProvider.meterRegistry)
}

object Workspace {
  // Flag if auto-run activities should be started automatically
  def autoRunCachedActivities: Boolean = {
    val cfg = DefaultConfig.instance()
    cfg.getBoolean("caches.config.enableAutoRun")
  }
}
