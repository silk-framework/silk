package de.fuberlin.wiwiss.silk.linkspec.input

import de.fuberlin.wiwiss.silk.instance.Instance

trait Input
{
  def apply(instances : Traversable[Instance]) : Traversable[String]
}