package de.fuberlin.wiwiss.silk.impl.writer

import de.fuberlin.wiwiss.silk.output.{Link, Formatter}

class NTriplesFormatter(val params : Map[String, String] = Map.empty) extends Formatter
{
    override def format(link : Link, predicateUri : String) = "<" + link.sourceUri + ">  <" + predicateUri + ">  <" + link.targetUri + "> .\n"
}