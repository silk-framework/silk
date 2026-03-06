package org.silkframework.rule.plugins.transformer.extraction

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.conditional.IfMatchesRegexTransformer
import org.silkframework.rule.plugins.transformer.replace.RegexReplaceTransformer
import org.silkframework.rule.plugins.transformer.selection.RegexSelectTransformer
import org.silkframework.rule.plugins.transformer.validation.ValidateRegex
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

/**
  * Extracts values from a String based on a regular expression.
  */
@Plugin(
  id = RegexExtractionTransformer.pluginId,
  categories = Array("Extract"),
  label = "Regex extract",
  description = "Extracts one or all matches of a regular expression within the input." +
    " If the regular expression contains one or more capturing groups, only the first group will be considered.",
  documentationFile = "RegexExtractionTransformer.md",
  relatedPlugins = Array(
    new PluginReference(
      id = RegexReplaceTransformer.pluginId,
      description = "Instead of extracting matching parts from a string, this plugin replaces them with a given replacement string."
    ),
    new PluginReference(
      id = RegexReplaceTransformer.pluginId,
      description = "The Regex extract plugin returns what the regular expression matches, or the first capturing group if capturing groups exist. The Regex replace plugin returns the full input string after rewriting it by replacing every match with the configured replacement."
    ),
    new PluginReference(
      id = RegexSelectTransformer.pluginId,
      description = "The Regex selection plugin does not return matched text at all. It emits copies of a provided output value at the positions where the checked values match the regular expressions, while the Regex extract plugin returns the matched substring or capturing-group content."
    ),
    new PluginReference(
      id = IfMatchesRegexTransformer.pluginId,
      description = "The If matches regex plugin uses the match only as a decision about which provided input value to return. The Regex extract plugin uses the match as the produced content, so the output is derived from the matched region rather than from branch inputs."
    ),
    new PluginReference(
      id = ValidateRegex.pluginId,
      description = "The Validate regex plugin keeps the original value only when the full value matches the configured regular expression and otherwise fails validation. The Regex extract plugin returns match-derived output and can return an empty result when nothing matches."
    ),
  )
)
@TransformExamples(Array(
  new TransformExample(
    description = "returns only the first match, when extractAll = false (default)",
    parameters = Array("regex", "[a-z]{2,4}123"),
    input1 = Array("afe123_abcd23"),
    output = Array("afe123")
  ),
  new TransformExample(
    description = "returns all matches, when extractAll = true",
    parameters = Array("regex", "[a-z]{2,4}123", "extractAll", "true"),
    input1 = Array("afe123_abcd123"),
    output = Array("afe123", "abcd123")
  ),
  new TransformExample(
    description = "returns an empty list if nothing matches",
    parameters = Array("regex", "^[a-z]{2,4}123"),
    input1 = Array("abcde123"),
    output = Array()
  ),
  new TransformExample(
    description = "returns the match of the first capturing group, which includes two to four letters",
    parameters = Array("regex", "^([a-z]{2,4})123([a-z]+)"),
    input1 = Array("abcd123xyz"),
    output = Array("abcd")
  ),
  new TransformExample(
    description = "returns the match of the first capturing group, which includes at least one letter",
    parameters = Array("regex", "^([a-z]+)123([a-z]{2,4})"),
    input1 = Array("pqrstuvwxyz123abcd"),
    output = Array("pqrstuvwxyz")
  ),
  new TransformExample(
    description = "returns an empty string, because the first capturing group includes the possibility of no letters",
    parameters = Array("regex", "^([a-z]*)123([a-z]{2,4})"),
    input1 = Array("123abcd"),
    output = Array("")
  ),
  new TransformExample(
    description = "returns an empty list, because the first capturing group excludes the possibility of no letters",
    parameters = Array("regex", "^([a-z]+)123([a-z]{2,4})"),
    input1 = Array("123abcd"),
    output = Array()
  ),

  new TransformExample(
    parameters = Array("regex", "\"bedeutungen\"\\s*:\\s*\\[\\s*(?:\"([^\"]*)\"(?:\\s*,\\s*\"([^\"]*)\")*)*\\s*\\]"),
    input1 = Array("\"bedeutungen\" : [ ]"),
    output = Array()
  )
))
case class RegexExtractionTransformer(
  @Param("Regular expression")
  regex: String,
  @Param("If true, all matches are extracted. If false, only the first match is extracted (default).")
  extractAll: Boolean = false) extends Transformer {

  lazy val r = regex.r

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.flatten.flatMap(matchValue)
  }

  private def matchValue(value: String): Seq[String] = {
    val matched = r.findAllIn(value).matchData.flatMap { m =>
      if(m.groupCount <1) {
        Option(m.matched)
      } else {
        Option(m.group(1))
      }
    }
    if(extractAll) {
      matched.toSeq
    } else if(matched.hasNext) {
      Seq(matched.next())
    } else {
      Seq.empty
    }
  }
}

object RegexExtractionTransformer {
  final val pluginId = "regexExtract"
}