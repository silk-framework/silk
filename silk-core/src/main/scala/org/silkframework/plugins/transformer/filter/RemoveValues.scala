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

package org.silkframework.plugins.transformer.filter

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.rule.input.Transformer

@Plugin(
  id = "removeValues",
  categories = Array("Filter"),
  label = "Remove values",
  description = "Removes values that contain words from a blacklist. The blacklist values are separated with ,"
)
case class RemoveValues(blacklist: String) extends Transformer {
  val filterValues = blacklist.split(",").map(_.toLowerCase).toSet

  override def apply(values: Seq[Set[String]]) = {
    values.head.filterNot(v => filterValues.contains(v.toLowerCase))
  }
}