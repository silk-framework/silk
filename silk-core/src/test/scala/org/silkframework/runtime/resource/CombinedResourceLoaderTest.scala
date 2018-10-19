package org.silkframework.runtime.resource

import java.io.{ByteArrayInputStream, InputStream}
import java.time.Instant

import org.scalatest.{FlatSpec, ShouldMatchers}

class CombinedResourceLoaderTest extends FlatSpec with ShouldMatchers {

  behavior of "CombinedResourceLoader"

  private val resource1 = new TestResource("test1", "contents 1")

  private val resource2 = new TestResource("test2", "contents 2")

  private val loader =
    new CombinedResourceLoader(
      resources = List(resource1),
      children = Map("child" -> new CombinedResourceLoader(resources = List(resource2)))
    )

  it should "load resources as provided in the constructor" in {
    loader.list shouldBe List("test1")
    loader.listChildren shouldBe List("child")
    loader.get("test1").loadAsString shouldBe "contents 1"
    loader.child("child").get("test2").loadAsString shouldBe "contents 2"
  }

  private class TestResource(val name: String, contents: String) extends Resource {
    override def path: String = "dummy path"
    override def exists: Boolean = true
    override def size: Option[Long] = None
    override def modificationTime: Option[Instant] = None
    override def inputStream: InputStream = new ByteArrayInputStream(contents.getBytes)
  }

}
