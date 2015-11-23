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

import org.silkframework.rule.input.{TransformInput, Transformer}

case class TransformNode(isSource: Boolean, inputs: List[InputNode], transformer: FunctionNode[Transformer]) extends InputNode {
  override val children = transformer :: inputs

  override def updateChildren(children: List[Node]) = {
    val newInputs = children.collect {
      case c: InputNode => c
    }
    val newTransformer = children.collect {
      case c: FunctionNode[Transformer] @unchecked => c
    }.head

    TransformNode(isSource, newInputs, newTransformer)
  }

  def build = {
    TransformInput(
      inputs = inputs.map(_.build),
      transformer = transformer.build()
    )
  }
}

object TransformNode {
  def load(input: TransformInput, isSource: Boolean) = {
    val inputNodes = input.inputs.map(i => InputNode.load(i, isSource)).toList
    val transformerNode = FunctionNode.load(input.transformer, Transformer)

    TransformNode(isSource, inputNodes, transformerNode)
  }
}
