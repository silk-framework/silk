package de.fuberlin.wiwiss.silk.impl.writer

import de.fuberlin.wiwiss.silk.output.{Link, Formatter}
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "ntriples", label = "N-Triples")
class NTriplesFormatter() extends Formatter
{
  override def format(link : Link, predicateUri : String) = "<" + link.source + ">  <" + predicateUri + ">  <" + link.target + "> .\n"
}