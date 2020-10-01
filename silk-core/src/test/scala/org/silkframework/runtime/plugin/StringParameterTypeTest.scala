package org.silkframework.runtime.plugin

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.plugin.StringParameterType.IntOptionType

class StringParameterTypeTest extends FlatSpec with MustMatchers {
  behavior of "String parameter type"

  it should "detect which parameter type to use based on class" in {
    StringParameterType.forTypeOpt(classOf[IntOptionParameter]) mustBe Some(IntOptionType)
  }
}
