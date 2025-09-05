package org.silkframework.rule.plugins.transformer.extraction

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

/**
  * Extracts values from a String based on a regular expression.
  */
@Plugin(
  id = "regexExtract",
  categories = Array("Extract"),
  label = "Regex extract",
  description = "Extracts one or all matches of a regular expression within the input." +
    " If the regular expression contains one or more capture groups, only the first matching group will be considered.",
  documentationFile = "RegexExtractionTransformer.md"
),
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
    description = "returns the match of the first capture group, which includes two to four letters",
    parameters = Array("regex", "^([a-z]{2,4})123([a-z]+)"),
    input1 = Array("abcd123xyz"),
    output = Array("abcd")
  ),
  new TransformExample(
    description = "returns the match of the first capture group, which includes at least one letter",
    parameters = Array("regex", "^([a-z]+)123([a-z]{2,4})"),
    input1 = Array("pqrstuvwxyz123abcd"),
    output = Array("pqrstuvwxyz")
  ),
  new TransformExample(
    description = "returns an empty string, because the first capture group includes the possibility of no letters",
    parameters = Array("regex", "^([a-z]*)123([a-z]{2,4})"),
    input1 = Array("123abcd"),
    output = Array("")
  ),
  new TransformExample(
    description = "returns an empty list, because the first capture group excludes the possibility of no letters",
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
