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

package org.silkframework.rule.plugins.transformer.normalize

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.StringUtils.toStringUtils

@Plugin(
  id = "camelCase",
  categories = Array("Normalize"),
  label = "Camel case",
  description = "Converts a string to camel case. Upper camel case is the default, lower camel case can be chosen."
)
@TransformExamples(Array(
  new TransformExample(
    description = "A sentence with several words is converted to a single word written in UpperCamelCase.",
    parameters = Array("isDromedary", "false"),
    input1 = Array("hello world"),
    output = Array("HelloWorld")
  ),
  new TransformExample(
    description = "A sentence with several words is converted to a single word written in lowerCamelCase.",
    parameters = Array("isDromedary", "true"),
    input1 = Array("hello world"),
    output = Array("helloWorld")
  ),
  new TransformExample(
    description = "A single lowercase letter is converted to UpperCamelCase, i.e. capitalized.",
    parameters = Array("isDromedary", "false"),
    input1 = Array("h"),
    output = Array("H")
  ),
  new TransformExample(
    description = "A single lowercase letter is converted to lowerCamelCase (aka. dromedary case), i.e. uncapitalized.",
    parameters = Array("isDromedary", "true"),
    input1 = Array("h"),
    output = Array("h")
  ),
  new TransformExample(
    description = "An empty space is removed. The dromedary/lower case is irrelevant here.",
    parameters = Array("isDromedary", "true"),
    input1 = Array(" "),
    output = Array("")
  ),
  new TransformExample(
    description = "An empty space is removed. The upper case is irrelevant here.",
    parameters = Array("isDromedary", "false"),
    input1 = Array(" "),
    output = Array("")
  ),
))
/**
 * Transformer for upper or lower camel case.
 *
 * @param isDromedary Parameter for choosing between upper camel case and lower camel case aka. dromedary case.
 */
case class CamelCaseTransformer(isDromedary: Boolean = false) extends SimpleTransformer {
  override def evaluate(value: String): String =
    if (isDromedary) value.lowerCamelCase
    else value.upperCamelCase
}
