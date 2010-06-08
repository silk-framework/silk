package de.fuberlin.wiwiss.silk.output.formatters

import de.fuberlin.wiwiss.silk.output.{Link, Formatter}

class NTriplesFormatter extends Formatter
{
    override def format(link : Link) = "<" + link.sourceUri + ">  <" + link.predicate + ">  <" + link.targetUri + "> .\n"
}
