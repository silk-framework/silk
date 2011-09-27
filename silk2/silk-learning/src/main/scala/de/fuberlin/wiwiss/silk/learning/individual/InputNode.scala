package de.fuberlin.wiwiss.silk.learning.individual

import de.fuberlin.wiwiss.silk.linkagerule.input.{TransformInput, PathInput, Input}

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
