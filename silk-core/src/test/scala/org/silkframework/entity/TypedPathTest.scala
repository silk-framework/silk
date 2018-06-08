package org.silkframework.entity

import org.scalatest.FlatSpec
import org.silkframework.util.{Uri, XmlSerializationHelperTrait}

class TypedPathTest extends FlatSpec with XmlSerializationHelperTrait {
  behavior of "TypePath"

  it should "serialize and deserialize correctly" in {
    val typedPath = TypedPath(
      path = Path("http://prop"),
      valueType = CustomValueType("http://someType"),
      isAttribute = true
    )
    testRoundTripSerialization(typedPath)
  }

  it should "serialize and deserialize non uris with slashes correctly" in {
    val typedPath = TypedPath(
      path = Path("fdshsdj//Português"),
      valueType = CustomValueType("http://someType"),
      isAttribute = true
    )
    testRoundTripSerialization(typedPath)
  }

  it should "equal paths with all kind of different serializations" in{
    val basePath = Path("http://example.org/file?query")

    basePath mustEqual Uri.parse("<http://example.org/file?query>")
    basePath mustEqual Uri.parse("http://example.org/file?query")
    basePath mustEqual Uri.parse("/<http://example.org/file?query>")
    basePath mustEqual Uri.parse("/http://example.org/file?query")
  }
}
