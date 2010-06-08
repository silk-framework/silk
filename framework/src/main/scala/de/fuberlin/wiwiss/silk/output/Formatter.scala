package de.fuberlin.wiwiss.silk.output

import formatters.NTriplesFormatter

/**
 * Serializes a link.
 */
trait Formatter
{
    def header : String = ""
    def footer : String = ""

    def format(link : Link) : String
}

/**
 * Formatter factory
 */
object Formatter
{
    /**
     * Creates a new formatter for a specific format
     */
    def apply(format : String) = format.replace("-", "").toLowerCase match
    {
        case "ntriples" => new NTriplesFormatter
        case _ => throw new IllegalArgumentException("Unsupported format " + format)
    }
}
