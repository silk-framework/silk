package org.silkframework.rule.plugins.transformer.filter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.rule.plugins.transformer.filter.RemoveStopWordsTest._
import org.silkframework.util.MockServerTestTrait

import scala.io.Source

class RemoveStopWordsTest extends AnyFlatSpec with Matchers with MockServerTestTrait {
  "RemoveStopWords" should "be case insensitive" in {
    val transformer = new RemoveStopWords()
    transformer.apply(Seq(Seq("To be or not to be", "that is the question"))).map(_.trim) should
      equal(Seq("", "question"))

    transformer.apply(Seq(Seq("It always seems impossible", "until it's done"))).map(_.trim) should
      equal(Seq("impossible", ""))
  }

  "RemoveStopWords" should "be case insensitive for upper case stop words and lower case values" in {
    val transformer = new RemoveStopWords(stopWords = upperCaseStopWords)
    transformer.apply(Seq(lowerCaseStopWords.toSeq)).map(_.trim) should equal(List.fill(lowerCaseStopWords.size)(""))
  }

  "RemoveStopWords" should "be case insensitive for lower case stop words and upper case values" in {
    val transformer = new RemoveStopWords(stopWords = lowerCaseStopWords)
    transformer.apply(Seq(upperCaseStopWords.toSeq)).map(_.trim) should equal(List.fill(upperCaseStopWords.size)(""))
  }
}

object RemoveStopWordsTest {
  val STOP_WORDS_LOWERCASE = "stopWords-de.txt"
  val STOP_WORDS_UPPERCASE = "stopwords-de-upper.txt"

  lazy val upperCaseStopWords: Set[String] = loadStopWords(STOP_WORDS_UPPERCASE)
  lazy val lowerCaseStopWords: Set[String] = loadStopWords(STOP_WORDS_LOWERCASE)

  def loadStopWords(file: String): Set[String] = {
    val txt = Source.fromResource(s"org/silkframework/rule/plugins/transformer/filter/$file")
    try {
      txt.getLines.toSet
    } finally {
      txt.close()
    }
  }
}
