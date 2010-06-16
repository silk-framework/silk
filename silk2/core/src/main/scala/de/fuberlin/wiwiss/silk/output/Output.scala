package de.fuberlin.wiwiss.silk.output

import outputs.{MemoryOutput, FileOutput}
import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}

/**
 * Represents an abstraction over an output of links.
 *
 * Implementing classes of this trait must override the write method.
 */
trait Output extends Strategy
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

object Output extends Factory[Output]
{
    register("file", classOf[FileOutput])
    register("memory", classOf[MemoryOutput])
}
