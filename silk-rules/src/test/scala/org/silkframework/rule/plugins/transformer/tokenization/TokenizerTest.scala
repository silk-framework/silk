package org.silkframework.rule.plugins.transformer.tokenization

import org.silkframework.test.PluginTest

class TokenizerTest extends PluginTest {

  override protected def pluginObject = Tokenizer()

  it should "split words" in {
    val tokenizer = Tokenizer("\\s")
    tokenizer(Seq(Seq("Hello World"))) shouldBe Seq("Hello", "World")
  }
}
