package org.silkframework.entity

import org.scalatest.FlatSpec
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.util.{Uri, XmlSerializationHelperTrait}

class TypedPathTest extends FlatSpec with XmlSerializationHelperTrait {
  behavior of "TypePath"

  it should "serialize and deserialize correctly" in {
    val typedPath = TypedPath(
      path = UntypedPath("http://prop"),
      valueType = CustomValueType("http://someType"),
      isAttribute = true
    )
    testRoundTripSerialization(typedPath)
  }

  it should "serialize and deserialize non uris with slashes correctly" in {
    val typedPath = TypedPath(
      path = UntypedPath("fdshsdj//Português"),
      valueType = CustomValueType("http://someType"),
      isAttribute = true
    )
    testRoundTripSerialization(typedPath)
  }

  it should "equal paths with all kind of different serializations" in{
    val basePath = UntypedPath("http://example.org/file?query")

    UntypedPath(Uri.parse("<http://example.org/file?query>")).equals(basePath) mustBe true
    UntypedPath(Uri.parse("http://example.org/file?query")).equals(basePath) mustBe true
    UntypedPath.parse("<http://example.org/file?query>").equals(basePath) mustBe true
    UntypedPath.parse("/<http://example.org/file?query>").equals(basePath) mustBe true
  }

  it should "equal paths ignoring the value type if equalsUntyped is used with one of the value types being the UntypedValueType" in {
    tp(ValueType.STRING).equalsUntyped(tp(ValueType.INTEGER)) mustBe false
    tp(ValueType.UNTYPED).equalsUntyped(tp(ValueType.INTEGER)) mustBe true
    tp(ValueType.INTEGER).equalsUntyped(tp(ValueType.UNTYPED)) mustBe true
    tp(ValueType.UNTYPED).equalsUntyped(tp(ValueType.UNTYPED)) mustBe true
  }

  private def tp(valueType: ValueType): TypedPath = TypedPath("alwaysTheSame", valueType)
}