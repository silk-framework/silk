package de.fuberlin.wiwiss.silk.output

import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}

/**
 * Serializes a link.
 */
trait Formatter extends Strategy
{
    def header : String = ""
    def footer : String = ""

    def format(link : Link, predicate : String) : String
}

/**
 * Formatter factory
 */
object Formatter extends Factory[Formatter]
