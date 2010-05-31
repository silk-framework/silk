package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance
import path.Path

class PathParam(pathStr : String) extends AnyParam
{
    val path = Path.parse(pathStr)

    override def evaluate(sourceInstance : Instance, targetInstance : Instance) = Set()
}