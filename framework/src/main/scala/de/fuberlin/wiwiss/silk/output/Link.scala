package de.fuberlin.wiwiss.silk.output

class Link(val sourceUri : String, val predicate : String, val targetUri : String, val confidence : Double)
{
    override def toString = "<" + sourceUri + ">  <" + predicate + ">  <" + targetUri + "> (" + confidence + ")" 
}
