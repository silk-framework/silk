package org.silkframework.rule.plugins.transformer.conditional

import org.silkframework.rule.test.TransformerTest

class ContainsAllOfTest extends TransformerTest[ContainsAllOf] {
  it should "return 'false' if the first input is empty and the second input is non-empty" in {
    ContainsAllOf().apply(Seq(Seq(), Seq("D"))) shouldBe Seq("false")
  }
}