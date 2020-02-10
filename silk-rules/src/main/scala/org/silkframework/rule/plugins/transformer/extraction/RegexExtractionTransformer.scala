package org.silkframework.rule.plugins.transformer.extraction

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

/**
  * Extract a single value from a String based on a regular expression.
  */
@Plugin(
  id = "regexExtract",
  categories = Array("Extract"),
  label = "Regex extract",
  description = "Extracts first occurrence of a regex \"regex\" in a string. If there is at least one capture group, it will return the string of the first capture group instead.")
case class RegexExtractionTransformer(regex: String) extends Transformer {
  lazy val r = regex.r

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.flatten.flatMap(matchValue)
  }

  private def matchValue(value: String): Option[String] = {
    val matched = r.findAllIn(value).matchData.map { m =>
      if(m.groupCount <1) {
        m.matched
      } else {
        m.group(1)
      }
    }
    matched.toSeq.headOption
  }
}