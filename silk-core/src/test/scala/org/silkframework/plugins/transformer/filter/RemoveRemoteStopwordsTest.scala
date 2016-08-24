package org.silkframework.plugins.transformer.filter

import org.scalatest.{Matchers, FlatSpec}

/**
  * Created by Christian Wartner on 12.08.2016.
  */
class RemoveRemoteStopwordsTest extends FlatSpec with Matchers {
  val transformer = new RemoveRemoteStopwords("https://sites.google.com/site/kevinbouge/stopwords-lists/stopwords_de.txt?attredirects=0&d=1")

  "RemoveRemoteStopwordsTransformer" should "return 'x'" in {
    transformer.apply(Seq(Seq("x des und sei"))).map(_.trim) should equal(Seq("x"))
  }

}
