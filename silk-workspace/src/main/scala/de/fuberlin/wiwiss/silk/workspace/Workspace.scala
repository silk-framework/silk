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

import java.io._
import java.util.logging.Logger
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import de.fuberlin.wiwiss.silk.runtime.resource.FileResourceManager
import de.fuberlin.wiwiss.silk.util.FileUtils._
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetModulePlugin
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingModulePlugin
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformModulePlugin
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowModulePlugin

class Workspace(file : File) {

  private val logger = Logger.getLogger(classOf[Workspace].getName)

  val resourceManager = new FileResourceManager(file)

  private val provider = new FileWorkspaceProvider(resourceManager)
  provider.registerModule(new DatasetModulePlugin())
  provider.registerModule(new LinkingModulePlugin())
  provider.registerModule(new TransformModulePlugin())
  provider.registerModule(new WorkflowModulePlugin())

  private var cacbedProjects = loadProjects()

  def projects: Seq[Project] = cacbedProjects

  /**
   * Retrieves a project by name.
   *
   * @throws java.util.NoSuchElementException If no project with the given name has been found
   */
  def project(name: Identifier): Project = {
    projects.find(_.name == name).getOrElse(throw new NoSuchElementException("Project '" + name + "' not found"))
  }

  def createProject(name: Identifier) = {
    require(!cacbedProjects.exists(_.name == name), "A project with the name '" + name + "' already exists")

    val projectConfig = ProjectConfig(name)
    provider.putProject(projectConfig)
    val newProject = new Project(projectConfig, provider, resourceManager.child(name))
    cacbedProjects :+= newProject
    newProject
  }

  def removeProject(name: Identifier) = {
    (file + ("/" + name)).deleteRecursive()
    cacbedProjects = cacbedProjects.filterNot(_.name == name)
  }

  def exportProject(name: Identifier, outputStream: OutputStream) {
    // Open ZIP
    val zip = new ZipOutputStream(outputStream)
    val projectDir = file + ("/" + name)
    require(projectDir.exists, s"Project $name does not exist.")

    // Recursively lists all files in the given directory
    def listFiles(file: File): List[File] = {
      if(file.isFile) file :: Nil
      else file.listFiles.toList.flatMap(listFiles)
    }

    // Go through all files and create a ZIP entry for each
    for(file <- listFiles(projectDir)) {
      val relativePath = projectDir.toPath.relativize(file.toPath).toString.replace("\\", "/")
      zip.putNextEntry(new ZipEntry(relativePath))
      val in = new BufferedInputStream(new FileInputStream(file))
      var b = in.read()
      while (b > -1) {
        zip.write(b)
        b = in.read()
      }
      in.close()
      zip.closeEntry()
    }

    // Close ZIP
    zip.close()
  }

  //TODO if an import fails, delete all already created files!
  def importProject(name: Identifier, inputStream: InputStream) {
    // Open ZIP
    val zip = new ZipInputStream(inputStream)
    val projectDir = file + ("/" + name)
    require(!projectDir.exists, s"Project $name already exists.")

    // Read all ZIP entries
    var entry = zip.getNextEntry
    while(entry != null) {
      if(!entry.isDirectory) {
        val file = projectDir + ("/" + entry.getName)
        file.getParentFile.mkdirs()
        val out = new BufferedOutputStream(new FileOutputStream(file))
        var b = zip.read()
        while (b > -1) {
          out.write(b)
          b = zip.read()
        }
        out.close()
      }
      zip.closeEntry()
      entry = zip.getNextEntry
    }

    // Close ZIP and reload
    zip.close()
    reload()
  }

  def reload() {
    cacbedProjects = loadProjects()
  }

  private def loadProjects(): Seq[Project] = {
    for(projectConfig <- provider.readProjects()) yield {
      logger.info("Loading project: " + projectConfig.id)
      new Project(projectConfig, provider, resourceManager.child(projectConfig.id))
    }
  }
}