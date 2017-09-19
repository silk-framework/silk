package org.silkframework.runtime.serialization

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.CustomTask
import org.silkframework.runtime.plugin.PluginRegistry

/**
  *
  */
class XmlSerializationTest extends FlatSpec with MustMatchers {
  behavior of "XML serialization"

  it should "serialize and read parameters" in {
    PluginRegistry.registerPlugin(classOf[TestCustomTask])
    implicit val readContext: ReadContext = ReadContext()
    val task = TestCustomTask("Some\nString\n  \t!!", 42)
    val node = XmlSerialization.toXml[CustomTask](task)
    val roundTripTask = XmlSerialization.fromXml[CustomTask](node)
    task mustBe roundTripTask
  }
}

case class TestCustomTask(param1: String, param2: Int) extends CustomTask {
  override def inputSchemataOpt = ???

  override def outputSchemaOpt = ???
}