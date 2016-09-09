package org.silkframework.plugins.transformer.conditional

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.Plugin

/**
  * Created on 9/9/16.
  */
@Plugin(
  id = "ifMatchesRegex",
  label = "if matches regex",
  categories = Array("Conditional"),
  description =
    """
       Accepts two or three inputs.
       If any value of the first input matches the regex, the second input is forwarded.
       Otherwise, the third input is forwarded (if present)."""
)
case class IfMatchesRegexTransformer(regex: String, negate: Boolean = false) extends Transformer {
  val r = regex.r
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    require(values.size >= 2, "ifMatchesRegex operator needs at least two inputs!")
    if(regexMatches(values.head)) {
      values(1)
    } else {
      values.drop(2).headOption.getOrElse(Seq())
    }
  }

  private def regexMatches(values: Seq[String]): Boolean = {
    val matches = if(!negate) {
      values.filter(str => regexMatches(str)).take(1)
    } else {
      values.filterNot(str => regexMatches(str)).take(1)
    }
    matches.nonEmpty
  }

  private def regexMatches(value: String): Boolean = {
    r.findFirstMatchIn(value).isDefined
  }
}
