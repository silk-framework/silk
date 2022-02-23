package org.silkframework.rule

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.util.XmlSerializationHelperTrait

class RuleLayoutTest extends FlatSpec with MustMatchers with XmlSerializationHelperTrait {
  behavior of "Rule layout"

  it should "serialize and deserialize" in {
    val layout = RuleLayout(
      Map(
        "nodeA" -> (1, 2),
        "nodeB" -> (3, 4),
        "nodeC" -> (5, 6)
      )
    )
    testRoundTripSerialization(layout)
  }
}
