package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.util.{MockServerTestTrait, ServedContent}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

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

  "RemoveRemoteStopWordsTransformer" should "be case insensitive" in {
    val transformer = RemoveRemoteStopWordsTransformer()
    transformer.apply(Seq(Seq("To be or not to be", "that is the question"))).map(_.trim) should
      equal(Seq("", "question"))

    transformer.apply(Seq(Seq("It always seems impossible", "until it's done"))).map(_.trim) should
      equal(Seq("impossible", ""))
  }
}
