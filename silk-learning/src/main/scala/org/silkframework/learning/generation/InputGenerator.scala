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

import org.silkframework.entity.paths.UntypedPath
import org.silkframework.learning.individual.{FunctionNode, InputNode, PathInputNode, TransformNode}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.DPair

import scala.util.Random

/**
 * Generates random inputs.
 */
case class InputGenerator(input: InputNode, useTransformations: Boolean)
                         (implicit pluginContext: PluginContext){

  /** The maximum number of transformations */
  val maxTransformations = 2

  /** The transformers to be used */
  val transformers = IndexedSeq("lowerCase", "stripUriPrefix", "tokenize")

  /**
   * Generates a new random input.
   */
  def apply(random: Random): InputNode = {
    if(useTransformations)
      transform(input, random.nextInt(maxTransformations + 1), random)
    else
      input
  }

  /**
   * Prepends a number of transformations to an input.
   *
   * @param input The input node
   * @param count The number of transformations to prepend
   */
  private def transform(input: InputNode, count: Int, random: Random): InputNode = {
    if(count == 0)
      input
    else {
      val transformer = transformers(random.nextInt(transformers.size))
      val transformedInput = TransformNode(input.isSource, input :: Nil, FunctionNode(transformer, Nil, Transformer))
      transform(transformedInput, count - 1, random)
    }
  }
}

object InputGenerator {

  def fromPathPair(pathPair: DPair[UntypedPath], useTransformations: Boolean)
                  (implicit context: PluginContext) = {
    DPair(
      source = new InputGenerator(PathInputNode(pathPair.source, true), useTransformations),
      target = new InputGenerator(PathInputNode(pathPair.target, false), useTransformations)
    )
  }

  def fromInputPair(inputPair: DPair[InputNode], useTransformations: Boolean)
                   (implicit context: PluginContext) = {
    DPair(
      source = new InputGenerator(inputPair.source, useTransformations),
      target = new InputGenerator(inputPair.target, useTransformations)
    )
  }
}