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

package org.silkframework.workspace.io

import org.silkframework.config._
import org.silkframework.dataset.{Dataset, DatasetTask}
import org.silkframework.rule.{LinkSpec, LinkingConfig, RuntimeLinkingConfig}
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project

/**
 * Builds a Silk configuration from the current Linking Task.
 */
object SilkConfigExporter {
  def build(project: Project, linkSpec: Task[LinkSpec]): LinkingConfig = {
    val datasets = project.tasks[Dataset]

    def findDataset(id: Identifier): DatasetTask = {
      new DatasetTask(id, datasets.find(_.id == id).get.data)
    }

    LinkingConfig(
      prefixes = project.config.prefixes,
      runtime = new RuntimeLinkingConfig(),
      sources = linkSpec.dataSelections.map(_.inputId).map(findDataset).toSeq,
      linkSpecs = linkSpec :: Nil,
      outputs = linkSpec.outputs.map(findDataset)
    )
  }
}