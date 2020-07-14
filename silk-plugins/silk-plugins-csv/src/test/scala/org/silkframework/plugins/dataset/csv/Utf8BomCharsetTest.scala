package org.silkframework.plugins.dataset.csv

import org.scalatest.{FlatSpec, Matchers}

class Utf8BomCharsetTest extends FlatSpec with Matchers {

  behavior of "Utf8BomCharset"

  it should "write byte order marks" in {
    // Specify charset by name to make sure that it has been registered correctly
    val bytes = "abc".getBytes("UTF-8-BOM")

    bytes.length shouldBe 6
    bytes(0) shouldBe 0xEF.asInstanceOf[Byte]
    bytes(1) shouldBe 0xBB.asInstanceOf[Byte]
    bytes(2) shouldBe 0xBF.asInstanceOf[Byte]
  }

}
