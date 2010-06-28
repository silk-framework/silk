package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance

trait Operator
{
    val weight : Int
    
    def apply(sourceInstance : Instance, targetInstance : Instance) : Traversable[Double]
}
