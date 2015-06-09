package de.fuberlin.wiwiss.silk.plugins.transformer.conditional

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

@Plugin(
  id = "ifContains",
  label = "if contains",
  categories = Array("Conditional"),
  description = "Accepts two or three inputs. If the first input contains the given value, the second input is forwarded. Otherwise, the third input is forwarded (if present)."
)
case class IfContains(search: String) extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    require(values.size >= 2, "The ifContains transformation accepts two or three inputs")
    if(values(0).exists(_.contains(search)))
      values(1)
    else
      if(values.size >= 3) values(2) else Set.empty
  }
}