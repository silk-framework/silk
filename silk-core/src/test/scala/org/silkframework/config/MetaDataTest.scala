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

}
