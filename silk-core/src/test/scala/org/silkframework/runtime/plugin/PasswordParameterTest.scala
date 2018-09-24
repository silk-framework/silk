package org.silkframework.runtime.plugin

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.plugin.ParameterType.PasswordParameterType

class PasswordParameterTest extends FlatSpec with MustMatchers {
  behavior of "password parameter"

  it should "store the encrypted password" in {
    val password = "some secret password"
    val passwordParameter = PasswordParameterType.fromString(password)
    passwordParameter.str must not include password
    passwordParameter.toString must startWith (PasswordParameterType.PREAMBLE)
    passwordParameter.decryptedString mustBe password

    PasswordParameterType.fromString(passwordParameter.toString).decryptedString mustBe password
  }
}
