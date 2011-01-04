package de.fuberlin.wiwiss.silk.linkspec.input

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

trait Input
{
  def apply(instances : SourceTargetPair[Instance]) : Traversable[String]
}