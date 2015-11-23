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

package org.silkframework.plugins.transformer.numeric

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.StringUtils.DoubleLiteral

/**
 * Counts the number of values.
 *
 * @author Robert Isele
 */
@Plugin(
  id = "count",
  categories = Array("Numeric"),
  label = "Count values",
  description =
    """Counts the number of values."""
)
case class CountTransformer() extends Transformer {

  def apply(values: Seq[Set[String]]): Set[String] = {
    Set(values.flatten.size.toString)
  }
}
