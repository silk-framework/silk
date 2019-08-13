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

import java.io._
import java.util.logging.Logger

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.resources.ResourceRepository

import scala.util.Try

/**
  * The workspace that manages loading of and access to workspace projects.
  *
  * @param provider - thw workspace provide
  * @param repository - the repository
  */
class Workspace(val provider: WorkspaceProvider, val repository: ResourceRepository) {

  private val log = Logger.getLogger(classOf[Workspace].getName)

  private var initialized = false

  @volatile
  private var cachedProjects: Seq[Project] = Seq.empty

  @volatile
  // Additional prefixes loaded from the workspace provider that will be added to every project
  private var additionalPrefixes: Prefixes = Prefixes.empty

  /** Load the projects of a user into the workspace. At the moment all users have access to all projects, so this is only,
    * executed once. */
  private def loadUserProjects()(implicit userContext: UserContext): Unit = synchronized {
    // FIXME: Extension for access control should happen here.
    if (!initialized) {
      loadProjects()
      initialized = true
    }
  }

  def projects(implicit userContext: UserContext): Seq[Project] = {
    loadUserProjects()
    cachedProjects
  }

  /**
   * Retrieves a project by name.
   *
   * @throws java.util.NoSuchElementException If no project with the given name has been found
   */
  def project(name: Identifier)(implicit userContext: UserContext): Project = {
    findProject(name).getOrElse(throw ProjectNotFoundException(name))
  }

  def findProject(name: Identifier)(implicit userContext: UserContext): Option[Project] = {
    loadUserProjects()
    projects.find(_.name == name)
  }

  def createProject(config: ProjectConfig)
                   (implicit userContext: UserContext): Project = synchronized {
    loadUserProjects()
    if(cachedProjects.exists(_.name == config.id)) {
      throw IdentifierAlreadyExistsException("Project " + config.id + " does already exist!")
    }
    provider.putProject(config)
    val newProject = new Project(config, provider, repository.get(config.id))
    cachedProjects :+= newProject
    newProject.setAdditionalPrefixes(additionalPrefixes)
    log.info(s"Created new project '${config.id}'. " + userContext.logInfo)
    newProject
  }

  def removeProject(name: Identifier)
                   (implicit userContext: UserContext): Unit = synchronized {
    loadUserProjects()
    project(name).activities.foreach(_.control.cancel())
    provider.deleteProject(name)
    cachedProjects = cachedProjects.filterNot(_.name == name)
    log.info(s"Removed project '$name'. " + userContext.logInfo)
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
    * @param name project name
    * @param file the file to read the project to import from
    * @param marshaller object that defines how the project should be unmarshaled.
    */
  def importProject(name: Identifier,
                    file: File,
                    marshaller: ProjectMarshallingTrait)
                   (implicit userContext: UserContext) {
    loadUserProjects()
    findProject(name) match {
      case Some(_) =>
        throw IdentifierAlreadyExistsException("Project " + name.toString + " does already exist!")
      case None =>
        marshaller.unmarshalProject(name, provider, repository.get(name), file)
        reloadProject(name)
        log.info(s"Imported project '$name'. " + userContext.logInfo)
    }
  }

  /**
    * Reloads this workspace.
    */
  def reload()(implicit userContext: UserContext): Unit = {
    loadUserProjects()

    // Stop all activities
    for{ project <- projects // Should not work directly on the cached projects
         activity <- project.activities } {
      activity.control.cancel()
    }
    // Refresh workspace provider
    provider.refresh()

    // Reload projects
    loadProjects()
  }

  /** Reloads the registered prefixes if the workspace provider supports this operation. */
  def reloadPrefixes()(implicit userContext: UserContext): Unit = {
    additionalPrefixes = provider.fetchRegisteredPrefixes()
    cachedProjects foreach { project =>
      project.setAdditionalPrefixes(additionalPrefixes)
    }
  }

  /** Reload a project from the backend */
  private def reloadProject(id: Identifier)
                           (implicit userContext: UserContext): Unit = synchronized {
    // remove project
    Try(project(id).activities.foreach(_.control.cancel()))
    cachedProjects = cachedProjects.filterNot(_.name == id)
    provider.readProject(id) match {
      case Some(projectConfig) =>
        val project = new Project(projectConfig, provider, repository.get(projectConfig.id))
        project.initTasks()
        cachedProjects :+= project
      case None =>
        log.warning(s"Project '$id' could not be reloaded in workspace, because it could not be read from the workspace provider!")
    }
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
      project.initTasks()
      project
    }
    reloadPrefixes()
  }
}