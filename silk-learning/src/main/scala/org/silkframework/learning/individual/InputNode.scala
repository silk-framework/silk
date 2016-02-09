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

import org.silkframework.rule.input.{Input, PathInput, TransformInput}

trait InputNode extends Node {
  def isSource: Boolean

  def build: Input
}

object InputNode {
  def load(input: Input, isSource: Boolean): InputNode = input match {
    case pathInput: PathInput => PathInputNode.load(pathInput, isSource)
    case transformInput: TransformInput => TransformNode.load(transformInput, isSource)
  }
}
