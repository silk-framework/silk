package org.silkframework.rule.plugins.transformer.filter

import java.net.UnknownHostException
import java.util.logging.Logger

import org.scalatest.{Matchers, FlatSpec}

/**
  * Created by Christian Wartner on 12.08.2016.
  */
class RemoveRemoteStopwordsTest extends FlatSpec with Matchers {
  lazy val transformer = new RemoveRemoteStopwords("https://sites.google.com/site/kevinbouge/stopwords-lists/stopwords_de.txt?attredirects=0&d=1")

  "RemoveRemoteStopwordsTransformer" should "return 'x'" in {
    try {
      transformer.apply(Seq(Seq("x des und sei"))).map(_.trim) should equal(Seq("x"))
    } catch {
      case e: UnknownHostException =>
        Logger.getLogger(getClass.getName).warning("No Internet connection, ignoring test.")
        // Ignore this test if no Internet connection available
    }
  }

}
