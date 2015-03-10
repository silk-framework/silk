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

import de.fuberlin.wiwiss.silk.config.{LinkSpecification, RuntimeConfig, LinkingConfig}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.workspace.Project

/**
 * Builds a Silk configuration from the current Linking Task.
 */
object SilkConfigExporter {
  def build(project: Project, linkSpec: LinkSpecification): LinkingConfig = {
    LinkingConfig(
      prefixes = project.config.prefixes,
      runtime = new RuntimeConfig(),
      sources = linkSpec.datasets.map(ds => project.tasks[Dataset].find(_.name == ds.datasetId).get.data).toSeq,
      linkSpecs = linkSpec :: Nil,
      outputs = Seq.empty
    )
  }
}