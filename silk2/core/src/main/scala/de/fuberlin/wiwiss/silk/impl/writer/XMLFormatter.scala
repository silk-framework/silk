package de.fuberlin.wiwiss.silk.impl.writer

import de.fuberlin.wiwiss.silk.output.{Link, Formatter}
import xml.{NodeSeq, Elem}

/**
 * Base trait of all formatters using XML.
 */
trait XMLFormatter extends Formatter
{
    override final def format(link : Link, predicateUri : String) : String = formatXML(link, predicateUri).toString + "\n"

    def formatXML(link : Link, predicateUri : String) : NodeSeq
}