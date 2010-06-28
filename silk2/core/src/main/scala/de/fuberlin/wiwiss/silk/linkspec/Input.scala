package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

trait Input
{
    def evaluate(instances : Traversable[Instance]) : Traversable[String]
}
