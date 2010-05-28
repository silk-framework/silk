package de.fuberlin.wiwiss.silk.linkspec

trait TransformParam extends AnyParam

object TransformParam
{
    def apply(function : String, params : Map[String, AnyParam]) : TransformParam =
    {
        throw new IllegalArgumentException("TransformParam with function " + function + " does not exist.")   
    }
}