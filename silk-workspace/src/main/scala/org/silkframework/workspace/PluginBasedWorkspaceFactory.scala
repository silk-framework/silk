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

import org.silkframework.config.{Config, DefaultConfig, Prefixes}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}
import org.silkframework.runtime.resource.EmptyResourceManager
import org.silkframework.workspace.resources.ResourceRepository

import java.time.Instant
import java.util.logging.{Level, Logger}
import javax.inject.Inject

class PluginBasedWorkspaceFactory extends WorkspaceFactory {

  override def workspace(implicit userContext: UserContext): Workspace = PluginBasedWorkspaceFactory.workspace

}

object PluginBasedWorkspaceFactory {

  private val log: Logger = Logger.getLogger(this.getClass.getName.stripSuffix("$"))

  @Inject
  private var configMgr: Config = DefaultConfig.instance

  @volatile
  private var timestamp: Instant = Instant.MIN

  private var _workspace: Option[Workspace] = None

  def workspace(implicit userContext: UserContext): Workspace = this.synchronized {
    _workspace match {
      case Some(w) if !timestamp.isBefore(configMgr.timestamp) => w
      case _ =>
        if(_workspace.nonEmpty) {
          log.info("Reloading workspace because the configuration has been updated.")
        }
        timestamp = configMgr.timestamp
        try {
          val w = initWorkspace
          _workspace = Some(w)
          w
        } catch {
          case ex: Exception => {
            Logger.getLogger(PluginBasedWorkspaceFactory.getClass.getName).log(Level.SEVERE, "Error loading workspace", ex)
            throw ex
          }
        }
    }
  }

  private def initWorkspace(implicit userContext: UserContext): Workspace = {
    implicit val pluginContext: PluginContext = PluginContext(Prefixes.empty, EmptyResourceManager(), user = userContext)

    // Load the workspace provider from configuration or use the default file-based one
    val provider: WorkspaceProvider =
      if (configMgr().hasPath("workspace.provider")) {
        val provider = PluginRegistry.createFromConfig[WorkspaceProvider]("workspace.provider")
        log.info("Using configured workspace provider " + configMgr().getString("workspace.provider.plugin"))
        provider
      } else {
        throw new RuntimeException("Workspace not configured, cannot initialize! Please configure 'workspace.provider.*'.")
      }

    val repository: ResourceRepository =
      if (configMgr().hasPath("workspace.repository")) {
        val repository = PluginRegistry.createFromConfig[ResourceRepository]("workspace.repository")
        log.info("Using configured workspace repository type " + configMgr().getString("workspace.repository.plugin"))
        repository
      } else {
        throw new RuntimeException("Workspace repository not configured, cannot initialize! Please configure 'workspace.repository.*'.")
      }

    // Create workspace
    new Workspace(provider, repository)
  }
}

