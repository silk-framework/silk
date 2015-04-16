package de.fuberlin.wiwiss.silk.plugins.transformer.conditional

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

@Plugin(
  id = "ifContains",
  label = "if contains",
  categories = Array("Conditional"),
  description = "Accepts two inputs. If the first input contains the given value, the second input is forwarded. Otherwise, no value is generated."
)
case class IfContains(search: String) extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    require(values.size == 2, "The ifContains transformation accepts exactly two inputs")
    if(values(0).exists(_.contains(search)))
      values(1)
    else
      Set.empty
  }
}