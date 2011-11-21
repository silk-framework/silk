/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.entity.Path
import util.Random
import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.learning.individual.{PathInputNode, InputNode, TransformNode, FunctionNode}

class InputGenerator(input: InputNode, useTransformations: Boolean) {

  val transformationProbability = 0.5

  val transformers = "lowerCase" :: "stripUriPrefix" :: "tokenize" :: Nil

  def apply(): InputNode = {
    if(useTransformations && Random.nextDouble < transformationProbability) {
      val transformer = transformers(Random.nextInt(transformers.size))
      TransformNode(input.isSource, input :: Nil, FunctionNode(transformer, Nil, Transformer))
    } else {
      input
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