package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

trait Input
{
    def evaluate(sourceInstance : Instance, targetInstance : Instance) : Traversable[String]
}