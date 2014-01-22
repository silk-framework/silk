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

package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.util.Identifier
import java.io.{InputStream, OutputStream}

trait Workspace {
  /**
   * Retrieves the projects in this workspace
   */
  def projects: Traversable[Project]

  /**
   * Retrieves a project by name.
   *
   * @throws java.util.NoSuchElementException If no project with the given name has been found
   */
  def project(name: Identifier): Project = {
    projects.find(_.name == name).getOrElse(throw new NoSuchElementException("Project '" + name + "' not found"))
  }

  /**
   * Creates a new project in this workspace.
   */
  def createProject(name: Identifier): Project

  /**
   * Removes a project from this workspace.
   */
  def removeProject(name: Identifier)

  /**
   * Exports a project to an output stream.
   */
  def exportProject(name: Identifier, outputStream: OutputStream)

  /**
   * Imports a project from an input stream.
   */
  def importProject(name: Identifier, inputStream: InputStream)

  /**
   * Reloads the workspace.
   */
  def reload()
}
