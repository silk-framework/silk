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

package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.FileUtils._
import de.fuberlin.wiwiss.silk.util.Identifier

class FileWorkspace(file : File) extends Workspace
{
  private val logger = Logger.getLogger(classOf[FileWorkspace].getName)

  file.mkdir()

  private var projectList : List[Project] =
  {
    for(projectDir <- file.listFiles.filter(_.isDirectory).toList) yield
    {
      logger.info("Loading project: " + projectDir)
      new FileProject(projectDir)
    }
  }

  override def projects : List[Project] = projectList

  override def createProject(name : Identifier) =
  {
    require(!projectList.exists(_.name == name), "A project with the name '" + name + "' already exists")

    val projectDir = (file + ("/" + name))
    projectDir.mkdir()
    val newProject = new FileProject(projectDir)
    projectList ::= newProject
    newProject
  }

  override def removeProject(name : Identifier) =
  {
    (file + ("/" + name)).deleteRecursive()
    projectList = projectList.filterNot(_.name == name)
  }
}