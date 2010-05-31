package de.fuberlin.wiwiss.silk.linkspec

trait Output

object Output
{
    def apply(outputType : String, params : Map[String, String]) : Output =
    {
        //Return dummy output
        return new Output() {}
        //throw new IllegalArgumentException("No Output " + outputType + " defined.")
    }
}