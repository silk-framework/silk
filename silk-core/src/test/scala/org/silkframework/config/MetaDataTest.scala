package org.silkframework.config

import MetaData.labelFromId
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class MetaDataTest extends AnyFlatSpec with Matchers {

  behavior of "MetaData"

  it should "generate user-friendly labels from identifiers" in {
    labelFromId("task") mustBe "task"
    labelFromId("myTask") mustBe "my Task"
    labelFromId("my_Task") mustBe "my Task"
    labelFromId("my_task") mustBe "my task"
    labelFromId("myABC") mustBe "my ABC"
  }

  it should "never return a label that is longer than the requested maximum" in {
    for(maxLength <- 6 to 60) {
      val label = "x" * 200
      val formatted = MetaData(label = Some(label)).formattedLabel("default", maxLength)
      withClue(s"for maxLength $maxLength: ") {
        formatted.length must be <= maxLength
      }
    }
  }

  it should "keep short labels unchanged and truncate long ones in the middle" in {
    MetaData(label = Some("short label")).formattedLabel("default", 50) mustBe "short label"
    val formatted = MetaData(label = Some("a" * 30 + "b" * 30)).formattedLabel("default", 20)
    formatted mustBe "aaaaaaa ... bbbbbbb"
    formatted.length must be <= 20
  }

}
