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

import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory}

/**
 * Transforms values.
 */
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
}

object Transformer extends PluginFactory[Transformer]
