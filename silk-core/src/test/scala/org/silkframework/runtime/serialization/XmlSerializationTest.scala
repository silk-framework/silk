package org.silkframework.runtime.serialization


import org.silkframework.config.CustomTask
import org.silkframework.runtime.plugin.PluginRegistry
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/**
  *
  */
class XmlSerializationTest extends AnyFlatSpec with Matchers {
  behavior of "XML serialization"

  it should "serialize and read parameters" in {
    PluginRegistry.registerPlugin(classOf[TestCustomTask])
    implicit val readContext: ReadContext = TestReadContext()
    val task = TestCustomTask("Some\nString\n  \t!!", 42)
    val node = XmlSerialization.toXml[CustomTask](task)
    val roundTripTask = XmlSerialization.fromXml[CustomTask](node)
    task mustBe roundTripTask
  }
}

case class TestCustomTask(param1: String, param2: Int) extends CustomTask {
  override def inputPorts = ???

  override def outputPort = ???
}