package de.fuberlin.wiwiss.silk.plugins.writer

import de.fuberlin.wiwiss.silk.output.Formatter
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Link

@Plugin(id = "ntriples", label = "N-Triples")
class NTriplesFormatter() extends Formatter {
  override def format(link: Link, predicateUri: String) = "<" + link.source + ">  <" + predicateUri + ">  <" + link.target + "> .\n"
}