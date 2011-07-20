package de.fuberlin.wiwiss.silk.impl.writer

import de.fuberlin.wiwiss.silk.output.{LinkWriter, Link}
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "memory", label = "Memory")
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
