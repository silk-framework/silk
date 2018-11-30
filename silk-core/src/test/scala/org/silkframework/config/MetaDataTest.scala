package org.silkframework.config

import org.scalatest.{FlatSpec, MustMatchers}
import MetaData.labelFromId

class MetaDataTest extends FlatSpec with MustMatchers {

  behavior of "MetaData"

  it should "generate user-friendly labels from identifiers" in {
    labelFromId("task") mustBe "task"
    labelFromId("myTask") mustBe "my Task"
    labelFromId("my_Task") mustBe "my Task"
    labelFromId("my_task") mustBe "my task"
    labelFromId("myABC") mustBe "my ABC"
  }

}
