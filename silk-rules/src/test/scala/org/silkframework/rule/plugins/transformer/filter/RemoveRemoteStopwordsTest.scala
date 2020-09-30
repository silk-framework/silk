package org.silkframework.rule.plugins.transformer.filter

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.util.{MockServerTestTrait, ServedContent}

/**
  * Created by Christian Wartner on 12.08.2016.
  */
class RemoveRemoteStopwordsTest extends FlatSpec with Matchers with MockServerTestTrait {

  "RemoveRemoteStopwordsTransformer" should "return 'x'" in {
    withAdditionalServer(Seq(
      ServedContent(
        contextPath = "/stopwords.txt",
        content = Some("the\nis\n")
      )
    )) { port =>
      val transformer = RemoveRemoteStopwords(s"http://localhost:$port/stopwords.txt")
      transformer.apply(Seq(Seq("the tree is big"))).map(_.trim) should equal(Seq("tree big"))
    }
  }

}
