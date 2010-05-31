package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

trait TransformParam extends AnyParam
{
    val params : Map[String, AnyParam] 
}

object TransformParam
{
    def apply(function : String, _params : Map[String, AnyParam]) : TransformParam =
    {
        //TODO add missing transforms
        //adding dummy transform until all transforms are available
        return new TransformParam
        {
            val params = _params

            def evaluate(sourceInstance : Instance, targetInstance : Instance) = Traversable()
        }
        
        //throw new IllegalArgumentException("TransformParam with function " + function + " does not exist.")
    }
}
