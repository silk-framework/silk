package de.fuberlin.wiwiss.silk.linkspec

trait LinkLimit

object LinkLimit
{
    def apply(max : Int, method : String) : LinkLimit =
    {
        throw new IllegalArgumentException("No LinkLimit " + method + " defined.")
    }
}