package org.silkframework.rule.plugins.transformer.validation

import org.silkframework.runtime.validation.ValidationException
import org.silkframework.test.PluginTest

class ValidateRegexTest extends PluginTest {

  override protected def pluginObject = ValidateRegex("\\w*")

  it should "accept matching values" in {
    noException should be thrownBy ValidateRegex("\\w*").evaluate("TestValue123")
    noException should be thrownBy ValidateRegex("[a-d]*").evaluate("abcd")
    noException should be thrownBy ValidateRegex("Prefix \\w\\w\\w").evaluate("Prefix abc")
  }

  it should "reject non-matching values" in {
    an [ValidationException] should be thrownBy ValidateRegex("\\w*").evaluate("(TestValue123)")
    an [ValidationException] should be thrownBy ValidateRegex("[a-d]*").evaluate("abcde")
    an [ValidationException] should be thrownBy ValidateRegex("Prefix \\w\\w\\w").evaluate("Prefixabc")
  }

}
