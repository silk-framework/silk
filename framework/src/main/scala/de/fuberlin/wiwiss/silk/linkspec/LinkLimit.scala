package de.fuberlin.wiwiss.silk.linkspec

trait LinkLimit

object LinkLimit
{
    def apply(max : Int, method : String) : LinkLimit =
    {
        //Return a dummy linkLimit
        new LinkLimit() {}
    }
}