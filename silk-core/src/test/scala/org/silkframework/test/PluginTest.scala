package org.silkframework.test

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import org.scalatest._
import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext, PluginRegistry, TestPluginContext}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Can be mixed in into a test to check for basic properties of a plugin.
  * Currently checks that:
  *  - The plugin is serializable (and the serialization is correct according to the plugins serialization operator)
  *  - The plugin is valid (e.g., the parameter types are allowed)
  */
abstract class PluginTest extends AnyFlatSpec with Matchers {

  /**
    * Use this plugin object for testing.
    */
  protected def pluginObject: AnyPlugin

  private val obj = pluginObject

  behavior of obj.getClass.getSimpleName

  it should "be serializable" in {
    obj shouldBe deserialize(serialize(obj))
  }

  it should "be a valid plugin" in {
    // Will throw an exception if the plugin is invalid
    PluginRegistry.reflect(obj)(TestPluginContext())
  }

  private def serialize(obj: Any): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(obj)
    oos.close()
    baos.toByteArray
  }

  private def deserialize(b: Array[Byte]): AnyRef = {
    val bais = new ByteArrayInputStream(b)
    val ois = new ObjectInputStream(bais)
    ois.readObject()
  }

}
