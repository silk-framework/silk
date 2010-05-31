package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

trait TransformParam extends AnyParam

object TransformParam
{
    def apply(function : String, params : Map[String, AnyParam]) : TransformParam =
    {
        //TODO add missing metrics
        //add dummy transform until all transforms are available
        return new TransformParam
        {
            def evaluate(sourceInstance : Instance, targetInstance : Instance) = Traversable()
        }
        
        //throw new IllegalArgumentException("TransformParam with function " + function + " does not exist.")
    }
}
