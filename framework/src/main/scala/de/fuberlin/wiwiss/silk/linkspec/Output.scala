package de.fuberlin.wiwiss.silk.linkspec

trait Output

object Output
{
    def apply(outputType : String, params : Map[String, String]) : Output =
    {
        throw new IllegalArgumentException("No Output " + outputType + " defined.")
    }
}