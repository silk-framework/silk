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

package de.fuberlin.wiwiss.silk.workspace.io

import de.fuberlin.wiwiss.silk.config.{RuntimeConfig, LinkingConfig}
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workspace.modules.output.OutputTask
import de.fuberlin.wiwiss.silk.workspace.modules.source.SourceTask

/**
 * Builds a Silk configuration from the current Linking Task.
 */
object SilkConfigExporter {
  def build(): LinkingConfig = {
    val project = User().project
    val linkSpec = User().linkingTask.linkSpec

    LinkingConfig(
      prefixes = project.config.prefixes,
      runtime = new RuntimeConfig(),
      sources = linkSpec.datasets.map(ds => project.tasks[SourceTask].find(_.name == ds.sourceId).get.source),
      linkSpecs = linkSpec :: Nil,
      outputs = project.tasks[OutputTask].map(_.output)
    )
  }
}