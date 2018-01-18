package org.silkframework.rule.plugins.transformer.conditional

import org.silkframework.rule.test.TransformerTest

class ContainsAnyOfTest extends TransformerTest[ContainsAnyOf] {
  it should "return 'false' if the first input is empty and the second input is non-empty" in {
    ContainsAllOf().apply(Seq(Seq(), Seq("D"))) shouldBe Seq("false")
  }
}