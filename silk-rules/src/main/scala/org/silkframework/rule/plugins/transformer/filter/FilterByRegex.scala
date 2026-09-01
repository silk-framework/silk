package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.InlineTransformer
import org.silkframework.rule.plugins.transformer.conditional.IfMatchesRegexTransformer
import org.silkframework.rule.plugins.transformer.extraction.RegexExtractionTransformer
import org.silkframework.rule.plugins.transformer.replace.RegexReplaceTransformer
import org.silkframework.rule.plugins.transformer.selection.RegexSelectTransformer
import org.silkframework.rule.plugins.transformer.validation.ValidateRegex
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.runtime.validation.ValidationException

import java.util.regex.{Matcher, Pattern, PatternSyntaxException}

/**
 * This transformer keeps or drops values based on a regular expression, matched in full by default or
 * against any substring when 'contains' is set; 'negate' inverts the keep/drop decision.
 */
@Plugin(
  id = FilterByRegex.pluginId,
  categories = Array("Filter"),
  label = "Filter by regex",
  description = "This transformer filters values by a regular expression: values matching the regex are kept, " +
    "unless 'negate' is set, which keeps only the values that don't. By default the regex must match a value " +
    "in full; setting 'contains' relaxes that so any value containing a match is kept instead.",
  documentationFile = "FilterByRegex.md",
  relatedPlugins = Array(
    new PluginReference(
      id = RegexSelectTransformer.pluginId,
      description = "Filter by regex keeps or drops values from the input sequence based on regex matching. " +
        "Regex selection keeps the checked value out of the output and instead returns a pattern-list-shaped result " +
        "filled with the provided output value where a pattern matches."
    ),
    new PluginReference(
      id = ValidateRegex.pluginId,
      description = "Validate regex throws on the first non-matching value, failing the whole operation. " +
        "Filter by regex just drops it and keeps the rest."
    ),
    new PluginReference(
      id = RegexReplaceTransformer.pluginId,
      description = "Filter by regex is a structural filter: it never changes a value's content, only whether " +
        "that value appears in the output. Regex replace works the other way: it changes what's inside each " +
        "value, but every value that goes in still comes out."
    ),
    new PluginReference(
      id = RegexExtractionTransformer.pluginId,
      description = "Regex extract never returns the original value — only part of the match itself, a substring " +
        "or a capturing group. By contrast, Filter by regex returns only the original value, unmodified, or " +
        "nothing at all."
    ),
    new PluginReference(
      id = IfMatchesRegexTransformer.pluginId,
      description = "If matches regex checks one sequence for a match, then returns one of two candidate " +
        "sequences, completely unchanged. Each value in a single sequence is tested on its own under Filter by " +
        "regex, kept or dropped to build the output."
    ),
    new PluginReference(
      id = FilterByLength.pluginId,
      description = "Filter by length only checks length, bounded by a minimum and a maximum. Length is " +
        "irrelevant to Filter by regex; only the pattern matters."
    ),
    new PluginReference(
      id = RemoveValues.pluginId,
      description = "Remove values checks each value against a fixed, comma-separated blacklist — an exact, " +
        "case-insensitive match, nothing computed. Filter by regex checks each value against a regular " +
        "expression instead, which can be a single word or a broad, open-ended pattern."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    description = "Keeps only values that fully match the regex.",
    parameters = Array("regex", "abc"),
    input1 = Array("abc", "xabcx", "xyz"),
    output = Array("abc")
  ),
  new TransformExample(
    description = "Keeps only values consisting of exactly three word characters.",
    parameters = Array("regex", "\\w\\w\\w"),
    input1 = Array("abc", "ab", "abcd", "a-x"),
    output = Array("abc")
  ),
  new TransformExample(
    description = "Excludes values that fully match the regex, keeping the rest.",
    parameters = Array("regex", "abc", "negate", "true"),
    input1 = Array("abc", "xabcx", "xyz"),
    output = Array("xabcx", "xyz")
  ),
  new TransformExample(
    description = "Keeps values that contain a match anywhere, not just full matches.",
    parameters = Array("regex", "abc", "contains", "true"),
    input1 = Array("abc", "xabcx", "xyz"),
    output = Array("abc", "xabcx")
  ),
  new TransformExample(
    description = "Excludes values that contain a match anywhere, keeping the rest.",
    parameters = Array("regex", "abc", "negate", "true", "contains", "true"),
    input1 = Array("abc", "xabcx", "xyz"),
    output = Array("xyz")
  ),
  new TransformExample(
    description = "A pattern matching zero characters finds a match in any value, so 'contains' keeps everything.",
    parameters = Array("regex", "a*", "contains", "true"),
    input1 = Array("xyz", "aaa", ""),
    output = Array("xyz", "aaa", "")
  ),
  new TransformExample(
    description = "An anchored pattern still only matches at the anchored position, even with 'contains' set.",
    parameters = Array("regex", "^abc", "contains", "true"),
    input1 = Array("abcxyz", "xabcx", "xyz"),
    output = Array("abcxyz")
  ),
  new TransformExample(
    description = "Throws when the regex fails to compile.",
    parameters = Array("regex", "("),
    input1 = Array("abc"),
    throwsException = classOf[PatternSyntaxException]
  ),
  new TransformExample(
    description = "A backslash-escaped metacharacter in the regex is matched literally.",
    parameters = Array("regex", "a\\.b"),
    input1 = Array("a.b", "aXb"),
    output = Array("a.b")
  ),
  new TransformExample(
    description = "A whitespace-only value matches a whitespace pattern.",
    parameters = Array("regex", "\\s+"),
    input1 = Array("   ", "abc"),
    output = Array("   ")
  ),
  new TransformExample(
    description = "A Unicode value matches a literal pattern correctly.",
    parameters = Array("regex", "café"),
    input1 = Array("café", "cafe"),
    output = Array("café")
  ),
  new TransformExample(
    description = "Returns nothing when the connected input carries no values.",
    parameters = Array("regex", "abc"),
    input1 = Array(),
    output = Array()
  ),
  new TransformExample(
    description = "Throws when no input is connected.",
    parameters = Array("regex", "abc"),
    throwsException = classOf[ValidationException]
  )
))
case class FilterByRegex(@Param(value = "The regular expression to test each value against.")
                          regex: String,
                          @Param(value = "If true, keeps values that do not match instead of values that do.")
                          negate: Boolean = false,
                          @Param(value = "If true, the pattern only needs to occur somewhere in the value.")
                          contains: Boolean = false) extends InlineTransformer {

  private lazy val pattern = Pattern.compile(regex)
  private def isMatch(str: String): Boolean = isMatch(pattern.matcher(str))
  private def isMatch(m: Matcher): Boolean = if (contains) m.find() else m.matches()

  private def negateIf[A](flag: Boolean)(f: A => Boolean): A => Boolean = if (flag) a => !f(a) else f

  override def apply(values: Seq[Seq[String]]): Seq[String] =
    values.headOption
      .getOrElse(throw new ValidationException("FilterByRegex requires at least one input."))
      .filter(negateIf(negate)(isMatch))
}

object FilterByRegex {
  final val pluginId = "filterByRegex"
}
