package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.input.SimpleTransformer

import scala.util.matching.Regex

abstract class RemoveStopWords(separator: String = "[\\s-]+") extends SimpleTransformer {
  def stopWords: Set[String]

  private val regex: Regex = separator.r

  override def evaluate(value: String): String = {
    val result = new StringBuilder
    for(word <- regex.split(value) if !stopWords.contains(word)) {
      result.append(word)
      result.append(" ")
    }
    result.toString()
  }
}
