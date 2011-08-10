package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.instance.Path
import util.Random
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.SourceTargetPair._
import de.fuberlin.wiwiss.silk.learning.individual.PathInputNode._
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.learning.individual.{PathInputNode, InputNode, TransformNode, StrategyNode}

class InputGenerator(input: InputNode) {

  val transformationProbability = 0.5

  val transformers = "lowerCase" :: "stripUriPrefix" :: "tokenize" :: Nil

  def apply(): InputNode = {
    if(Random.nextDouble < transformationProbability) {
      val transformer = transformers(Random.nextInt(transformers.size))
      TransformNode(input.isSource, input :: Nil, StrategyNode(transformer, Nil, Transformer))
    } else {
      input
    }
  }
}

object InputGenerator {

  def fromPathPair(pathPair: SourceTargetPair[Path]) = {
    SourceTargetPair(
      source = new InputGenerator(PathInputNode(pathPair.source, true)),
      target = new InputGenerator(PathInputNode(pathPair.target, false))
    )
  }

  def fromInputPair(inputPair: SourceTargetPair[InputNode]) = {
    SourceTargetPair(
      source = new InputGenerator(inputPair.source),
      target = new InputGenerator(inputPair.target)
    )
  }
}