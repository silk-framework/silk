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

package org.silkframework.rule.plugins.transformer.numeric

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = NumReduceTransformer.pluginId,
  categories = Array("Numeric"),
  label = "Numeric reduce",
  description = "Strips all non-numeric characters from a string.",
  relatedPlugins = Array(
    new PluginReference(
      id = AggregateNumbersTransformer.pluginId,
      description = "Numeric reduce strips non-numeric characters from each value. Aggregate numbers silently discards any value it cannot parse as a number, so values with embedded non-numeric characters are lost to the aggregation without this step."
    ),
    new PluginReference(
      id = NumOperationTransformer.pluginId,
      description = "Numeric reduce strips non-numeric characters from each value, making each one parseable as a number. Numeric operation throws a validation exception on any input that cannot be parsed, rather than discarding it silently."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("keepPunctuation", "false"),
    input1 = Array("some1.2Value"),
    output = Array("12")
  ),
  new TransformExample(
    parameters = Array("keepPunctuation", "true"),
    input1 = Array("some1.2Value"),
    output = Array("1.2")
  )
))
case class NumReduceTransformer(keepPunctuation: Boolean = true) extends SimpleTransformer {

  override def evaluate(value: String): String = {
    if(keepPunctuation)
      value.filter(c => c.isDigit || c == '.' || c == ',')
    else
      value.filter(_.isDigit)
  }

}

object NumReduceTransformer {
  final val pluginId = "numReduce"
}
