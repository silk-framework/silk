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

package de.fuberlin.wiwiss.silk.util.plugin

case class Parameter(name: String, dataType: Parameter.Type, description: String = "No description", defaultValue: Option[AnyRef] = None) {

  /**
   * Retrieves the current value of this parameter.
   */
  def apply(obj: AnyRef): AnyRef = {
    obj.getClass.getMethod(name).invoke(obj)
  }
}

object Parameter {

  object Type extends Enumeration {
    val String = Value("String")
    val Char = Value("Char")
    val Int = Value("Int")
    val Double = Value("Double")
    val Boolean = Value("Boolean")
    val Resource = Value("Resource")
  }

  type Type = Type.Value
}