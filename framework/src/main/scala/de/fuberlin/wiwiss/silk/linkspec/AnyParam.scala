package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

trait AnyParam
{
    def evaluate(sourceInstance : Instance, targetInstance : Instance) : String
}