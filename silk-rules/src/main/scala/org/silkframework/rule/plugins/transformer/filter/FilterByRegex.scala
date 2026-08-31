package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.InlineTransformer
import org.silkframework.rule.plugins.transformer.selection.RegexSelectTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

import java.util.regex.{Matcher, Pattern}

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
  relatedPlugins = Array(
    new PluginReference(
      id = RegexSelectTransformer.pluginId,
      description = "Filter by regex keeps or drops values from the input sequence based on regex matching. " +
        "Regex selection keeps the checked value out of the output and instead returns a pattern-list-shaped result " +
        "filled with the provided output value where a pattern matches."
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

  override def apply(values: Seq[Seq[String]]): Seq[String] = values.head.filter(negateIf(negate)(isMatch))
}

object FilterByRegex {
  final val pluginId = "filterByRegex"
}
