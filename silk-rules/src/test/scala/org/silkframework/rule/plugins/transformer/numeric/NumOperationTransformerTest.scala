package org.silkframework.rule.plugins.transformer.numeric

import org.silkframework.rule.test.TransformerTest

class NumOperationTransformerTest extends TransformerTest[NumOperationTransformer] {

  it should "return no value if all inputs are empty" in {
    NumOperationTransformer("+")(Seq(Seq.empty, Seq.empty)) shouldBe empty
  }
}
