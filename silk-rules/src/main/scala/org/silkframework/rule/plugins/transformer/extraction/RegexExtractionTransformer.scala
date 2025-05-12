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
  description = "Extracts occurrences of a regex \"regex\" in a string. If there is at least one capture group, it will return the string of the first capture group instead.")
@TransformExamples(Array(
  new TransformExample(
    description = "returns the first match",
    parameters = Array("regex", "[a-z]{2,4}123"),
    input1 = Array("afe123_abc123"),
    output = Array("afe123")
  ),
  new TransformExample(
    description = "returns all matches, if extractAll = true",
    parameters = Array("regex", "[a-z]{2,4}123", "extractAll", "true"),
    input1 = Array("afe123_abc123"),
    output = Array("afe123", "abc123")
  ),
  new TransformExample(
    description = "returns an empty list if nothing matches",
    parameters = Array("regex", "^[a-z]{2,4}123"),
    input1 = Array("abcdef123"),
    output = Array()
  ),
  new TransformExample(
    description = "returns the match of the first capture group that matches",
    parameters = Array("regex", "^([a-z]{2,4})123([a-z]+)"),
    input1 = Array("abcd123xyz"),
    output = Array("abcd")
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
  @Param("If true, all matches are extracted. If false, only the first match is extracted.")
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