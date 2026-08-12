package org.silkframework.rule.plugins.transformer.numeric

import org.silkframework.rule.test.TransformerTest

class NumOperationTransformerTest extends TransformerTest[NumOperationTransformer] {

  // Direct calls, since annotation examples treat an exception like an empty output and cannot guard these cases.
  it should "return no value if all inputs are empty" in {
    NumOperationTransformer("+")(Seq(Seq.empty, Seq.empty)) shouldBe empty
  }

  it should "return no value if any input is empty" in {
    NumOperationTransformer("-")(Seq(Seq("10"), Seq.empty)) shouldBe empty
  }
}
