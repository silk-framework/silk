package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

class Param(value : String) extends AnyParam
{
    override def evaluate(sourceInstance : Instance, targetInstance : Instance) = value
}