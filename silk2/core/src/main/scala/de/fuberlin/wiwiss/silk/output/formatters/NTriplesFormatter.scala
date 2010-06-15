package de.fuberlin.wiwiss.silk.output.formatters

import de.fuberlin.wiwiss.silk.output.{Link, Formatter}

class NTriplesFormatter extends Formatter
{
    override def format(link : Link, predicateUri : String) = "<" + link.sourceUri + ">  <" + predicateUri + ">  <" + link.targetUri + "> .\n"
}
