package de.fuberlin.wiwiss.silk.output.formatters

import de.fuberlin.wiwiss.silk.output.{Link, Formatter}
import xml.{NodeSeq, Elem}

/**
 * Base trait of all formatters using XML.
 */
trait XMLFormatter extends Formatter
{
    override final def format(link : Link) : String = formatXML(link).toString + "\n"

    def formatXML(link : Link) : NodeSeq
}
