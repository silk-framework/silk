package de.fuberlin.wiwiss.silk.workbench.learning.tree

import de.fuberlin.wiwiss.silk.instance.Path
import de.fuberlin.wiwiss.silk.linkspec.input.PathInput
case class PathInputNode(isSource: Boolean, path: Path) extends InputNode {
  def build = PathInput(path = path)
}

object PathInputNode {
  def load(input: PathInput, isSource: Boolean) = {
    PathInputNode(isSource, input.path)
  }
}
