package de.fuberlin.wiwiss.silk.linkspec

trait Output
{
    val params : Map[String, String]
}

object Output
{
    def apply(outputType : String, params : Map[String, String]) : Output =
    {
        //Return dummy output
        return new Output() { val params = Map[String, String]() }
        //throw new IllegalArgumentException("No Output " + outputType + " defined.")
    }
}