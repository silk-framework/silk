package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.input.SimpleTransformer

import scala.io.Source
import scala.util.matching.Regex

/**
 * Transformer which removes the stop words provided by its implementations, additionally to the default stop word list.
 *
 * @param separator Regular Expressions for the separator between single words.
 * @param defaultStopWords List of default stop words. If not set, a sensible default (= base) stop word list is used.
 */
abstract class RemoveStopWords(separator: String = "[\\s-]+",
                               defaultStopWords: Set[String] = RemoveStopWords.loadDefaultStopWords
                              ) extends SimpleTransformer {
  def stopWords: Set[String]

  private def allStopWords: Set[String] = defaultStopWords ++ stopWords

  private val regex: Regex = separator.r

  override def evaluate(value: String): String = {
    val result = new StringBuilder
    for(word <- regex.split(value) if !allStopWords.contains(word)) {
      result.append(word)
      result.append(" ")
    }
    result.toString()
  }
}

object RemoveStopWords {
  private def loadDefaultStopWords: Set[String] = {
    val txt = Source.fromResource("org/silkframework/rule/plugins/transformer/filter/stopWords.txt")
    try {
      txt.getLines.toSet
    } finally {
      txt.close()
    }
  }
}
