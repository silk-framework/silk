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

package org.silkframework.rule.plugins.transformer.replace

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "map",
  categories = Array("Replace"),
  label = "Map",
  description = "Replaces values based on a map of values."
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("map", "Key1:Value1,Key2:Value2", "default", "Undefined"),
    input1 = Array("Key1"),
    output = Array("Value1")
  ),
  new TransformExample(
    parameters = Array("map", "Key1:Value1,Key2:Value2", "default", "Undefined"),
    input1 = Array("Key1X"),
    output = Array("Undefined")
  )
))
case class MapTransformer(
  @Param(value = "A map of values", example = "A:1,B:2,C:3")
  map: Map[String, String],
  @Param("Default if the map defines no value")
  default: String) extends SimpleTransformer {

  override def evaluate(value: String): String = {
    map.getOrElse(value, default)
  }
}
