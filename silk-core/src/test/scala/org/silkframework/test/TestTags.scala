package org.silkframework.test

import org.scalatest.Tag

/**
  * Created on 7/22/16.
  */
object TestTags {
  object SlowTest extends Tag("silk.SlowTest")
  object BrowserTest extends Tag("silk.BrowserTest")
  object CoverageIncompatibleTest extends Tag("silk.CoverageIncompatibleTest")
}
