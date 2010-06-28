package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

trait Input
{
    def apply(instances : Traversable[Instance]) : Traversable[String]
}
