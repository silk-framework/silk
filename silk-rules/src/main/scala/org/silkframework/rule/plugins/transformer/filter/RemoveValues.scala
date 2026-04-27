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

import org.silkframework.rule.input.InlineTransformer
import org.silkframework.rule.plugins.transformer.normalize.RemoveDuplicates
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = RemoveValues.pluginId,
  categories = Array("Filter"),
  label = "Remove values",
  description = "Removes values that contain words from a blacklist. The blacklist values are separated with commas.",
  relatedPlugins = Array(
    new PluginReference(
      id = RemoveEmptyValues.pluginId,
      description = "Remove values works from a blacklist: any value matching a word in that list is dropped. Remove empty values has no such list; it removes only empty strings."
    ),
    new PluginReference(
      id = RemoveDuplicates.pluginId,
      description = "The two plugins filter on different grounds. Remove values drops a value because of what it is; Remove duplicates drops a value because it already appeared earlier in the sequence."
    )
  )
)
case class RemoveValues(blacklist: String) extends InlineTransformer {
  val filterValues = blacklist.split(",").map(_.toLowerCase).toSet

  override def apply(values: Seq[Seq[String]]) = {
    values.head.filterNot(v => filterValues.contains(v.toLowerCase))
  }
}

object RemoveValues {
  final val pluginId = "removeValues"
}