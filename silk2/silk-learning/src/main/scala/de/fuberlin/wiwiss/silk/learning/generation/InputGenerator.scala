package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.instance.Path
import util.Random
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.learning.individual.{PathInputNode, InputNode, TransformNode, StrategyNode}

class InputGenerator(input: InputNode, useTransformations: Boolean) {

  val transformationProbability = 0.5

  val transformers = "lowerCase" :: "stripUriPrefix" :: "tokenize" :: Nil

  def apply(): InputNode = {
    if(useTransformations && Random.nextDouble < transformationProbability) {
      val transformer = transformers(Random.nextInt(transformers.size))
      TransformNode(input.isSource, input :: Nil, StrategyNode(transformer, Nil, Transformer))
    } else {
      input
    }
  }
}

object InputGenerator {

  def fromPathPair(pathPair: SourceTargetPair[Path], useTransformations: Boolean) = {
    SourceTargetPair(
      source = new InputGenerator(PathInputNode(pathPair.source, true), useTransformations),
      target = new InputGenerator(PathInputNode(pathPair.target, false), useTransformations)
    )
  }

  def fromInputPair(inputPair: SourceTargetPair[InputNode], useTransformations: Boolean) = {
    SourceTargetPair(
      source = new InputGenerator(inputPair.source, useTransformations),
      target = new InputGenerator(inputPair.target, useTransformations)
    )
  }
}