package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.StringParameterType.StringMapType
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class StringMapTypeTest extends AnyFlatSpec with Matchers {

  behavior of "Map[String] plugin parameter"

  implicit val prefixes: Prefixes = Prefixes.empty

  implicit val context: PluginContext = PluginContext.empty

  it should "parse basic map expressions" in {
    StringMapType.fromString("a:b,c:d") mustBe Map("a" -> "b", "c" -> "d")
    StringMapType.fromString("key;1:value") mustBe Map("key;1" -> "value")
    StringMapType.fromString("1:2,3:4,5:6") mustBe Map("1" -> "2", "3" -> "4", "5" -> "6")
  }

  it should "parse map expressions with encoded reserved characters" in {
    StringMapType.fromString("a%3Ax:b%2Cy,c%2Cz:d%3Aw") mustBe Map("a:x" -> "b,y", "c,z" -> "d:w")
  }

  it should "parse an empty map expression" in {
    StringMapType.fromString("") mustBe Map()
  }

  it should "serialize basic maps" in {
    StringMapType.toString(Map("a" -> "b", "c" -> "d")) mustBe "a:b,c:d"
  }

  it should "serialize maps with reserved characters" in {
    StringMapType.toString(Map("a:x" -> "b,y", "c,z" -> "d:w")) mustBe "a%3Ax:b%2Cy,c%2Cz:d%3Aw"
  }

  it should "serialize an empty map" in {
    StringMapType.toString(Map()) mustBe ""
  }

}
