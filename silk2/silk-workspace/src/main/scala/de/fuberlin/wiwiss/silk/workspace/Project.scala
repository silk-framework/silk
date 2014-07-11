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

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceManager
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingModuleProvider
import de.fuberlin.wiwiss.silk.workspace.modules.output.OutputModuleProvider
import de.fuberlin.wiwiss.silk.workspace.modules.source.SourceModuleProvider
import de.fuberlin.wiwiss.silk.workspace.modules.transform._
import de.fuberlin.wiwiss.silk.workspace.modules.{ModuleTask, ModuleConfig, Module, ModuleProvider}
import scala.xml.XML

/**
 * Implementation of a project which is stored on the local file system.
 */
class Project(val name: Identifier, resourceManager: ResourceManager) {

  private implicit val logger = Logger.getLogger(classOf[Project].getName)

  val resources = resourceManager.child("resources")

  private var cachedConfig: Option[ProjectConfig] = None

  /**
   * Reads the project configuration.
   */
  def config = {
    if(cachedConfig.isEmpty) {
      if(resourceManager.list.contains("config.xml")) {
        val configXML = XML.load(resourceManager.get("config.xml").load)
        val prefixes = Prefixes.fromXML(configXML \ "Prefixes" head)
        cachedConfig = Some(ProjectConfig(prefixes))
      } else {
        cachedConfig = Some(ProjectConfig.default)
      }
    }

    cachedConfig.get
  }

  /**
   * Writes the updated project configuration.
   */
  def config_=(config : ProjectConfig) {
    val configXMl =
      <ProjectConfig>
      { config.prefixes.toXML }
      </ProjectConfig>

    resourceManager.put("config.xml") { os => configXMl.write(os) }
    cachedConfig = Some(config)
  }

  /**
   * The source module, which encapsulates all data sources.
   */
  val sourceModule = module("source", new SourceModuleProvider())

  /**
   * The linking module, which encapsulates all linking tasks.
   */
  val linkingModule = module("linking", new LinkingModuleProvider())

  /**
   * The transform module, which encapsulates all linking tasks.
   */
  val transformModule = module("transform", new TransformModuleProvider())

  /**
   * The output module, which encapsulates all output tasks.
   */
  val outputModule = module("output", new OutputModuleProvider())

  /**
   * Creates a new module from a module provider.
   */
  private def module[C <: ModuleConfig, T <: ModuleTask](name: String, provider: ModuleProvider[C,T]) = {
    new Module(provider, resourceManager.child(name), this)
  }
}