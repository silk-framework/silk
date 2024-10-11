package org.silkframework.rule


import org.silkframework.util.XmlSerializationHelperTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class RuleLayoutTest extends AnyFlatSpec with Matchers with XmlSerializationHelperTrait {
  behavior of "Rule layout"

  it should "serialize and deserialize" in {
    val layout = RuleLayout(
      Map(
        "nodeA" -> NodePosition(1, 2),
        "nodeB" -> NodePosition(3, 4, Some(200)),
        "nodeC" -> NodePosition(5, 6)
      )
    )
    testRoundTripSerialization(layout)
  }
}
