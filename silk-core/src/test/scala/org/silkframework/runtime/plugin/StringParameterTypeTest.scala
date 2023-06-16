package org.silkframework.runtime.plugin

import org.silkframework.runtime.plugin.StringParameterType.IntOptionType
<<<<<<< HEAD
import org.silkframework.runtime.plugin.types.IntOptionParameter

class StringParameterTypeTest extends FlatSpec with MustMatchers {
  behavior of "String parameter type"

  it should "detect which parameter type to use based on class" in {
    StringParameterType.forTypeOpt(classOf[IntOptionParameter]) mustBe Some(IntOptionType)
  }
}
=======
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class StringParameterTypeTest extends AnyFlatSpec with Matchers {
  behavior of "String parameter type"

  it should "detect which parameter type to use based on class" in {
    StringParameterType.forTypeOpt(classOf[IntOptionParameter]) mustBe Some(IntOptionType)
  }
}
>>>>>>> develop
