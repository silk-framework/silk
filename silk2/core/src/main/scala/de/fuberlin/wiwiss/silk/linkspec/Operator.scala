package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

trait Operator
{
    val weight : Int
    
    def evaluate(sourceInstance : Instance, targetInstance : Instance) : Traversable[Double]
}
