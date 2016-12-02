package org.silkframework.entity

import org.scalatest.FlatSpec
import org.silkframework.util.XmlSerializationHelperTrait

/**
  * Created on 11/23/16.
  */
class TypedPathTest extends FlatSpec with XmlSerializationHelperTrait {
  behavior of "TypePath"

  it should "serialize and deserialize correctly" in {
    val typedPath = TypedPath(
      path = Path("http://prop"),
      valueType = CustomValueType("http://someType")
    )
    testRoundTripSerialization(typedPath)
  }
}
