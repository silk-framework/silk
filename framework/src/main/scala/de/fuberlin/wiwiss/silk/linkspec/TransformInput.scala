package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

trait TransformInput extends Input
{
    val inputs : Seq[Input]
    val params : Map[String, String]
}

object TransformInput
{
    def apply(function : String, _inputs : Seq[Input], _params : Map[String, String]) : TransformInput =
    {
        //TODO add missing transforms
        //adding dummy transform until all transforms are available
        return new TransformInput
        {
            val inputs = _inputs

            val params = _params

            def evaluate(sourceInstance : Instance, targetInstance : Instance) = Traversable()
        }
        
        //throw new IllegalArgumentException("TransformParam with function " + function + " does not exist.")
    }
}
