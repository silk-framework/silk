package org.silkframework.test

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.scalatest.{FlatSpec, _}
import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.PluginRegistry

/**
  * Can be mixed in into a test to check for basic properties of a plugin.
  * Currently checks that:
  *  - The plugin is serializable (and the serialization is correct according to the plugins serialization operator)
  *  - The plugin is valid (e.g., the parameter types are allowed)
  */
abstract class PluginTest extends FlatSpec with ShouldMatchers {

  /**
    * Use this plugin object for testing.
    */
  protected def pluginObject: AnyRef

  private val obj = pluginObject

  behavior of obj.getClass.getSimpleName

  it should "be serializable" in {
    obj shouldBe unserialize(serialize(obj))
  }

  it should "be a valid plugin" in {
    // Will throw an exception if the plugin is invalid
    PluginRegistry.reflect(obj)(Prefixes.empty)
  }

  private def serialize(obj: Any) = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(obj)
    oos.close()
    baos.toByteArray
  }

  private def unserialize(b: Array[Byte]) = {
    val bais = new ByteArrayInputStream(b)
    val ois = new ObjectInputStream(bais)
    ois.readObject()
  }

}
