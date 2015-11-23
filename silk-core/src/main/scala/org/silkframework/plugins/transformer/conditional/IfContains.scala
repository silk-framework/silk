package org.silkframework.plugins.transformer.conditional

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.Plugin

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