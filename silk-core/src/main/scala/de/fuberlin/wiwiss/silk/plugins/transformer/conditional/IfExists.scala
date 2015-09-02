package de.fuberlin.wiwiss.silk.plugins.transformer.conditional

import de.fuberlin.wiwiss.silk.rule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

@Plugin(
  id = "ifExists",
  label = "if exists",
  categories = Array("Conditional"),
  description = "Accepts two or three inputs. If the first input provides a value, the second input is forwarded. Otherwise, the third input is forwarded (if present)."
)
case class IfExists() extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    require(values.size >= 2, "The ifExists transformation requires at least two inputs")
    if(values(0).nonEmpty)
      values(1)
    else
      if(values.size >= 3) values(2) else Set.empty
  }
}