package de.fuberlin.wiwiss.silk.workbench.learning.tree

import de.fuberlin.wiwiss.silk.linkspec.input.{TransformInput, PathInput, Input}

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
