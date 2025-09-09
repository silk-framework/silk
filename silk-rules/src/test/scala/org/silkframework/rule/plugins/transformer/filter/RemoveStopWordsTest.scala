package org.silkframework.rule.plugins.transformer.filter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.rule.plugins.transformer.filter.RemoveStopWordsTest._
import org.silkframework.util.MockServerTestTrait

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.CollectionHasAsScala

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
    val transformed = transformer.apply(Seq(lowerCaseStopWords.toSeq)).map(_.trim)
    transformed should equal(List.fill(lowerCaseStopWords.size)(""))
    transformed.filter(_.nonEmpty) should equal(List.empty)
  }

  "RemoveStopWords" should "be case insensitive for lower case stop words and upper case values" in {
    val transformer = new RemoveStopWords(stopWords = lowerCaseStopWords)
    val transformed = transformer.apply(Seq(upperCaseStopWords.toSeq)).map(_.trim)
    transformed should equal(List.fill(upperCaseStopWords.size)(""))
    transformed.filter(_.nonEmpty) should equal(List.empty)
  }

  "RemoveStopWords" should "not just return empty values, if any uppercase stop words for a lowercase 'ß' are not 'ẞ' but 'SS'" in {
    val transformer = new RemoveStopWords(stopWords = badUpperCaseStopWords)
    val transformed = transformer.apply(Seq(lowerCaseStopWords.toSeq)).map(_.trim)
    transformed should not equal List.fill(lowerCaseStopWords.size)("")
    transformed.filter(_.nonEmpty) should not equal List.empty
  }

  "RemoveStopWords" should "return less empty values than the source, if the source contains repetitions with 'SS'" in {
    val transformer = new RemoveStopWords(stopWords = lowerCaseStopWords)
    val transformed = transformer.apply(Seq(badUpperCaseStopWords.toSeq)).map(_.trim)
    transformed should equal(List.fill(badUpperCaseStopWords.size)(""))
    transformed should not equal List.fill(lowerCaseStopWords.size)("")
    transformed.filter(_.nonEmpty) should equal(List.empty)
  }
}

object RemoveStopWordsTest {
  val STOP_WORDS_LOWERCASE = "stopWords-de.txt"
  val STOP_WORDS_UPPERCASE = "stopwords-de-upper.txt"
  val STOP_WORDS_UPPERCASE_BAD = "stopwords-de-upper-with-SS-mismatch.txt"

  lazy val lowerCaseStopWords: Set[String] = loadStopWords(STOP_WORDS_LOWERCASE)
  lazy val upperCaseStopWords: Set[String] = loadStopWords(STOP_WORDS_UPPERCASE)
  lazy val badUpperCaseStopWords: Set[String] = loadStopWords(STOP_WORDS_UPPERCASE_BAD)

  def loadStopWords(file: String): Set[String] =
    Files.readAllLines(Paths.get(getClass.getResource(file).toURI), Charset.forName("utf-8")).asScala.toSet
}
