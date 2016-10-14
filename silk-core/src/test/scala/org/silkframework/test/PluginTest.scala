package org.silkframework.test

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.scalatest.{FlatSpec, _}

/**
  * Can be mixed in into a test to check for basic properties of a plugin.
  * Currently only checks for serializability.
  */
abstract class PluginTest extends FlatSpec with ShouldMatchers {

  /**
    * Use this plugin object for testing.
    */
  protected def pluginObject: Any

  private val obj = pluginObject

  behavior of obj.getClass.getSimpleName

  it should "be serializable" in {
    obj shouldBe unserialize(serialize(obj))
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
