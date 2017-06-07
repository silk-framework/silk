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

import java.io.File
import java.util.logging.{Level, Logger}
import javax.inject.Inject

import org.silkframework.config.{Config, DefaultConfig}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.workspace.resources.{PerProjectFileRepository, ResourceRepository}
import org.silkframework.workspace.xml._

class FileUser extends User {

  override def workspace = FileUser.workspace

}

object FileUser {
  private val log: Logger = Logger.getLogger(this.getClass.getName.stripSuffix("$"))
  @Inject
  private var configMgr: Config = DefaultConfig.instance

  lazy val workspaceDir = {
    val elds_home = System.getenv("ELDS_HOME")
    if(elds_home != null)
      new File(elds_home + "/var/dataintegration/workspace/")
    else
      new File(System.getProperty("user.home") + "/.silk/workspace/")
  }

  lazy val workspace: Workspace = {
    try {
      // Load the workspace provider from configuration or use the default file-based one
      val provider: WorkspaceProvider =
        if(configMgr().hasPath("workspace.provider")) {
          val provider = PluginRegistry.createFromConfig[WorkspaceProvider]("workspace.provider")
          log.info("Using configured workspace provider " + configMgr().getString("workspace.provider.plugin"))
          provider
        } else {
          FileWorkspaceProvider(workspaceDir.getAbsolutePath)
        }

      val repository: ResourceRepository =
        if(configMgr().hasPath("workspace.repository")) {
          val repository = PluginRegistry.createFromConfig[ResourceRepository]("workspace.repository")
          log.info("Using configured workspace repository type " + configMgr().getString("workspace.repository.plugin"))
          repository
        } else {
          PerProjectFileRepository(workspaceDir.getAbsolutePath)
        }

      // Create workspace
      new Workspace(provider, repository)
    }
    catch {
      case ex: Exception => {
        Logger.getLogger(FileUser.getClass.getName).log(Level.SEVERE, "Error loading workspace", ex)
        throw ex
      }
    }
  }
}

