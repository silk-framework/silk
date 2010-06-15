package de.fuberlin.wiwiss.silk.output

import outputs.{MemoryOutput, FileOutput}

/**
 * Represents an abstraction over an output of links.
 *
 * Implementing classes of this trait must override the write method.
 */
trait Output
{
    val params : Map[String, String]

    /**
     * Initializes this output.
     */
    def open() : Unit = {}

    /**
     * Writes a new link to this output.
     */
    def write(link : Link, predicateUri : String) : Unit

    /**
     * Closes this output.
     */
    def close() : Unit = {}
}

object Output
{
    def apply(outputType : String, params : Map[String, String]) : Output =
    {
        outputType match
        {
            case "file" => new FileOutput(params)
            case "memory" => new MemoryOutput(params)
            case _ => throw new IllegalArgumentException("No Output " + outputType + " available.")
        }
    }
}
