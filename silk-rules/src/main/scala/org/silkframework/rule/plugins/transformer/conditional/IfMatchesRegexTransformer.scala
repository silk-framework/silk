package org.silkframework.rule.plugins.transformer.conditional

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "ifMatchesRegex",
  label = "If matches regex",
  categories = Array("Conditional"),
  description =
    """This transformer uses a regular expression as a matching condition, in order to distinguish which input to take.""",
  documentationFile = "IfMatchesRegexTransformer.md"
)
@TransformExamples(Array(
  new TransformExample(
    description = "returns the second input if the regex matches the first input",
    parameters = Array("regex", "[abc]{2}", "negate", "false"),
    input1 = Array("black"),
    input2 = Array("should be taken"),
    input3 = Array("last value should be left"),
    output = Array("should be taken")
  ),
  new TransformExample(
    description = "returns the third input if the regex does not match the first input",
    parameters = Array("regex", "[abc]{2}", "negate", "false"),
    input1 = Array("xyz"),
    input2 = Array("should be left"),
    input3 = Array("last value should be taken"),
    output = Array("last value should be taken")
  ),
  new TransformExample(
    description = "returns an empty value if the regex does not match the first input",
    parameters = Array("regex", "[abc]{2}", "negate", "false"),
    input1 = Array("xyz"),
    input2 = Array("should be left"),
    output = Array()
  ),
))
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
