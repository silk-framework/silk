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

package org.silkframework.learning.generation

import org.silkframework.entity.Path
import util.Random
import org.silkframework.rule.input.Transformer
import org.silkframework.util.DPair
import org.silkframework.learning.individual.{PathInputNode, InputNode, TransformNode, FunctionNode}

/**
 * Generates random inputs.
 */
case class InputGenerator(input: InputNode, useTransformations: Boolean) {

  /** The maximum number of transformations */
  val maxTransformations = 2

  /** The transformers to be used */
  val transformers = IndexedSeq("lowerCase", "stripUriPrefix", "tokenize")

  /**
   * Generates a new random input.
   */
  def apply(): InputNode = {
    if(useTransformations)
      transform(input, Random.nextInt(maxTransformations + 1))
    else
      input
  }

  /**
   * Prepends a number of transformations to an input.
   *
   * @param input The input node
   * @param count The number of transformations to prepend
   */
  private def transform(input: InputNode, count: Int): InputNode = {
    if(count == 0)
      input
    else {
      val transformer = transformers(Random.nextInt(transformers.size))
      val transformedInput = TransformNode(input.isSource, input :: Nil, FunctionNode(transformer, Nil, Transformer))
      transform(transformedInput, count - 1)
    }
  }
}

object InputGenerator {

  def fromPathPair(pathPair: DPair[Path], useTransformations: Boolean) = {
    DPair(
      source = new InputGenerator(PathInputNode(pathPair.source, true), useTransformations),
      target = new InputGenerator(PathInputNode(pathPair.target, false), useTransformations)
    )
  }

  def fromInputPair(inputPair: DPair[InputNode], useTransformations: Boolean) = {
    DPair(
      source = new InputGenerator(inputPair.source, useTransformations),
      target = new InputGenerator(inputPair.target, useTransformations)
    )
  }
}