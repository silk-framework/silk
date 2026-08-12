package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.Param

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.matching.Regex

/**
 * Transformer which removes the stop words provided by its implementations, additionally to the default stop word list.
 *
 * @param separator Regular Expressions for the separator between single words.
 * @param stopWords List of stop words. If not set, a sensible default (= base) stop word list is used.
 */
class RemoveStopWords(@Param(value = "RegEx for detecting words") separator: String = "[\\s-]+",
                      @Param(value = "Stop word list") stopWords: Set[String] = RemoveStopWords.loadDefaultStopWords)
  extends SimpleTransformer {
  private val regex: Regex = separator.r

  /** The stop words to remove. Subclasses may override this to defer expensive loading until the first evaluation. */
  protected def loadStopWords(): Set[String] = stopWords

  // The set is invariant, so it is lower-cased only once instead of once per word.
  @transient private lazy val lowerCaseStopWords: Set[String] = loadStopWords().map(_.toLowerCase)

  override def evaluate(value: String): String = {
    val result = new StringBuilder
    for(word <- regex.split(value) if !lowerCaseStopWords.contains(word.toLowerCase)) {
      result.append(word.toLowerCase)
      result.append(" ")
    }
    result.toString()
  }
}

object RemoveStopWords {
  private val STOP_WORDS_FILE: String = "stopWords.txt"

  private def loadDefaultStopWords: Set[String] =
    Files.readAllLines(Paths.get(getClass.getResource(STOP_WORDS_FILE).toURI), Charset.forName("utf-8")).asScala.toSet
}
