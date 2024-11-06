package org.silkframework.runtime.plugin

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.plugin.ParameterValueUtils.ExtendedParameterValues

class ParameterValueUtilsTest extends AnyFlatSpec with Matchers {

  behavior of "ParameterValueUtils"

  it should "retrieve nested parameters" in {
    val values =
      ParameterValues(Map(
        "count" -> ParameterStringValue("1"),
        "input" -> ParameterValues(Map(
          "id" -> ParameterStringValue("i1")
        ))
      ))

    implicit val context: PluginContext = PluginContext.empty

    values.valueAtPath("") shouldBe None
    values.valueAtPath("count") shouldBe Some("1")
    values.valueAtPath("input/id") shouldBe Some("i1")
  }

}
