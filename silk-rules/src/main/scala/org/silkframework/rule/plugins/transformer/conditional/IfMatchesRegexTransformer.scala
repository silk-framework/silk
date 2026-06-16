package org.silkframework.rule.plugins.transformer.conditional

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.InlineTransformer
import org.silkframework.rule.plugins.transformer.extraction.RegexExtractionTransformer
import org.silkframework.rule.plugins.transformer.selection.RegexSelectTransformer
import org.silkframework.rule.plugins.transformer.validation.ValidateRegex
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = IfMatchesRegexTransformer.pluginId,
  label = "If matches regex",
  categories = Array("Conditional"),
  description =
    """This transformer uses a regular expression as a matching condition, in order to distinguish which input to take.""",
  documentationFile = "IfMatchesRegexTransformer.md",
  relatedPlugins = Array(
    new PluginReference(
      id = ValidateRegex.pluginId,
      description = "A regular expression match drives both operators, but the outcome differs. The “If matches regex” plugin routes between alternative input values, while the “Validate regex” plugin draws an acceptance boundary by deciding whether the checked value is valid."
    ),
    new PluginReference(
      id = RegexSelectTransformer.pluginId,
      description = "The Regex selection plugin marks match positions by emitting copies of a provided output value wherever a regular expression matches the checked value sequence. The If matches regex plugin uses a regular expression match as a branch decision between alternative input values rather than producing positional markers."
    ),
    new PluginReference(
      id = RegexExtractionTransformer.pluginId,
      description = "The Regex extract plugin returns the matching content from the input string, or the first capturing group if the regular expression contains capturing groups. The If matches regex plugin does not return matched content; it uses the match only to choose which of the provided input values becomes the output."
    ),
  )
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
case class IfMatchesRegexTransformer(regex: String, negate: Boolean = false) extends InlineTransformer {
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

object IfMatchesRegexTransformer {
  final val pluginId = "ifMatchesRegex"
}
