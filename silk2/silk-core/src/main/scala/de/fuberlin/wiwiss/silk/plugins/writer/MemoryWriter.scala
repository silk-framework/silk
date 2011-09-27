package de.fuberlin.wiwiss.silk.plugins.writer

import de.fuberlin.wiwiss.silk.output.{LinkWriter, Link}
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "memory", label = "Memory")
case class MemoryWriter() extends LinkWriter {
  private var _links = List[Link]()

  def links = _links

  def clear() {
    _links = List[Link]()
  }

  override def write(link: Link, predicateUri: String): Unit = {
    _links ::= link
  }
}
