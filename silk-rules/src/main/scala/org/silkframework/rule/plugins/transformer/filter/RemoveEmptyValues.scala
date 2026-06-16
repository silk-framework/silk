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

package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.InlineTransformer
import org.silkframework.rule.plugins.transformer.value.EmptyValueTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = RemoveEmptyValues.pluginId,
  categories = Array("Filter"),
  label = "Remove empty values",
  description = "Removes empty values.",
  relatedPlugins = Array(
    new PluginReference(
      id = RemoveValues.pluginId,
      description = "Remove empty values removes only empty strings and has no parameters. Remove values is the configurable alternative, filtering out values that match words from a user-supplied blacklist."
    ),
    new PluginReference(
      id = EmptyValueTransformer.pluginId,
      description = "Empty value produces what Remove empty values removes: an empty sequence. Remove empty values is selective; Empty value is unconditional."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("value1", "", "value2"),
    output = Array("value1", "value2")
  ),
  new TransformExample(
    input1 = Array("", ""),
    output = Array()
  )
))
case class RemoveEmptyValues() extends InlineTransformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.head.filter(!_.isEmpty)
  }
}

object RemoveEmptyValues {
  final val pluginId = "removeEmptyValues"
}
