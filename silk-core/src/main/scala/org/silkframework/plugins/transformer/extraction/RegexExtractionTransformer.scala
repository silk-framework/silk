package org.silkframework.plugins.transformer.extraction

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

/**
  * Created on 3/7/16.
  */
@Plugin(
  id = "regexExtract",
  categories = Array("Extract"),
  label = "Regex extract",
  description = "Extracts first occurrence of a regex \"regex\" in a string. If there is at least one capture group, it will return the string of the first capture group instead.")
case class RegexExtractionTransformer(regex: String) extends RegexExtractionTransformerBase(regex)

class RegexExtractionTransformerBase(regex: String) extends SimpleTransformer {
  lazy val r = regex.r

  def this() {
    this("")
  }

  override def evaluate(value: String): String = {
    val matched = r.findAllIn(value).matchData.map { m =>
      if(m.groupCount <1) {
        m.matched
      } else {
        m.group(1)
      }
    }
    matched.toSeq.headOption.getOrElse("")
  }
}