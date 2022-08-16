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

package org.silkframework.learning.individual

import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext, PluginFactory}
import org.silkframework.util.IdentifierGenerator

case class FunctionNode[T <: AnyPlugin](id: String, parameters: List[ParameterNode], factory: PluginFactory[T])
                                       (implicit context: PluginContext) extends Node {

  def build(implicit identifiers: IdentifierGenerator = new IdentifierGenerator): T = {
    factory(id, parameters.map(p => (p.key, p.value)).toMap)
  }
}

object FunctionNode {
  def load[T <: AnyPlugin](plugin: T, factory: PluginFactory[T])
                          (implicit context: PluginContext): FunctionNode[T] = factory.unapply(plugin) match {
    case Some((pluginDesc, parameters)) => FunctionNode(pluginDesc.id, parameters.map {
      case (key, value) => ParameterNode(key, value)
    }.toList, factory)
    case None => throw new IllegalArgumentException()
  }
}