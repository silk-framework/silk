package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance
import path.Path

case class PathInput(path : Path) extends Input
{
    override def evaluate(sourceInstance : Instance, targetInstance : Instance) =
    {
        if(sourceInstance.variable == path.variable) sourceInstance.evaluate(path)
        else if(targetInstance.variable == path.variable) targetInstance.evaluate(path)
        else throw new IllegalArgumentException("No instance found with variable " + path.variable)
    }

    override def toString = "PathInput(" + path + ")"
}
