package de.fuberlin.wiwiss.silk.learning.individual

import de.fuberlin.wiwiss.silk.entity.Path
import de.fuberlin.wiwiss.silk.linkagerule.input.PathInput
case class PathInputNode(path: Path, isSource: Boolean) extends InputNode {
  def build = PathInput(path = path)
}

object PathInputNode {
  def load(input: PathInput, isSource: Boolean) = {
    PathInputNode(input.path, isSource)
  }
}
