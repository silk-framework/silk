package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.plugins.transformer.filter.RemoveRemoteStopWordsTransformerTest.defaultStopWordList
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{MockServerTestTrait, ServedContent}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.{Codec, Source}

/**
  * Created by Christian Wartner on 12.08.2016.
  */
class RemoveRemoteStopWordsTransformerTest extends AnyFlatSpec with Matchers with MockServerTestTrait {

  "RemoveRemoteStopWordsTransformer" should "return 'x'" in {
    withAdditionalServer(Seq(
      ServedContent(
        contextPath = "/stopwords.txt",
        content = Some("the\nis\n")
      )
    )) { port =>
      val transformer = RemoveRemoteStopWordsTransformer(s"http://localhost:$port/stopwords.txt")
      transformer.apply(Seq(Seq("the tree is big"))).map(_.trim) should equal(Seq("tree big"))
    }
  }

  "RemoveRemoteStopWordsTransformer" should "not access the URL at construction time and report a clear error on evaluation" in {
    // Construction must not throw, although the stop word list cannot be loaded.
    val transformer = RemoveRemoteStopWordsTransformer("file:///nonexistent-stop-word-list.txt")
    intercept[ValidationException] {
      transformer.evaluate("some value")
    }
  }

  "RemoveRemoteStopWordsTransformer" should "not re-attempt a failed stop word list download on every evaluation" in {
    val transformer = RemoveRemoteStopWordsTransformer("file:///nonexistent-stop-word-list.txt")
    val firstException = intercept[ValidationException](transformer.evaluate("some value"))
    // The cached failure is rethrown, so the same instance proves that no new download was attempted.
    val secondException = intercept[ValidationException](transformer.evaluate("another value"))
    secondException should be theSameInstanceAs firstException
  }

  "RemoveRemoteStopWordsTransformer" should "reject a malformed stop word list URL at construction time" in {
    intercept[ValidationException] {
      RemoveRemoteStopWordsTransformer("not a valid url")
    }
  }

  "RemoveRemoteStopWordsTransformer" should "be case insensitive" in {
    // The default stop word list is served locally, so that tests never access the public default URL.
    withAdditionalServer(Seq(
      ServedContent(
        contextPath = "/stopwords.txt",
        content = Some(defaultStopWordList)
      )
    )) { port =>
      val transformer = RemoveRemoteStopWordsTransformer(s"http://localhost:$port/stopwords.txt")
      transformer.apply(Seq(Seq("To be or not to be", "that is the question"))).map(_.trim) should
        equal(Seq("", "question"))

      transformer.apply(Seq(Seq("It always seems impossible", "until it's done"))).map(_.trim) should
        equal(Seq("impossible", ""))
    }
  }
}

object RemoveRemoteStopWordsTransformerTest {

  /** The default stop word list that is bundled with the RemoveStopWords transformer. */
  lazy val defaultStopWordList: String = {
    val source = Source.fromInputStream(getClass.getResourceAsStream("stopWords.txt"))(Codec.UTF8)
    try {
      source.mkString
    } finally {
      source.close()
    }
  }
}
