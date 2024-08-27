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

package org.silkframework.rule.input

import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory}
import org.silkframework.runtime.resource.Resource

/**
 * Transforms values.
 */
@PluginType(
  customDescription = classOf[TransformerDescriptionGenerator]
)
trait Transformer extends AnyPlugin {

  /**
    * Transforms a sequence of values
    *
    * @param values A sequence which contains as many elements as there are input operators for this transformation.
    *               For each input operator it contains a sequence of values.
    *
    * @return The transformed sequence of values.
    */
  def apply(values: Seq[Seq[String]]): Seq[String]

  /**
    * The resources that are directly referenced by this transformer.
    */
  def referencedResources: Seq[Resource] = Seq.empty

  /**
   * Called if a referenced resource has been updated.
   * The calls are done on a best-effort basis and there is not guarantee that this method is called for all updates.
   * In particular, it is only called if the resource is updated via Silk/DataIntegration and not for external updates.
   */
  def resourceUpdated(resource: Resource): Unit = {
    // Overwrite to handle updates
  }
}

object Transformer extends PluginFactory[Transformer]
