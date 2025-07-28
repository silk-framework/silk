package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.input.SimpleTransformer

import scala.io.{BufferedSource, Source}
import scala.util.matching.Regex

/**
 * Transformer which removes the stop words provided by its implementations.
 *
 * @param separator Regular Expressions for the separator between single words.
 */
abstract class RemoveStopWords(separator: String = "[\\s-]+") extends SimpleTransformer {
  private val defaultStopWords: Set[String] = {
    def loadStopWords: Set[String] = {
      val txt = Source.fromResource("stopWords.txt")
      try {
        txt.getLines.toSet
      } finally {
        txt.close()
      }
    }
    loadStopWords
  }

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
