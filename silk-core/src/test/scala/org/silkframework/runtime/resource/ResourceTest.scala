package org.silkframework.runtime.resource

import java.io.{ByteArrayInputStream, IOException, InputStream}
import java.time.Instant

import org.scalatest.{FlatSpec, MustMatchers}

class ResourceTest extends FlatSpec with MustMatchers {

  behavior of "Resource"

  it should "not allow large sources to be loaded into memory" in {
    val smallResource = new TestResource("small".getBytes, size = Some(10))
    val largeResource = new TestResource("large".getBytes, size = Some(1000000000))

    smallResource.loadAsString mustBe "small"
    intercept[IOException] {
      largeResource.loadAsString
    }
    intercept[IOException] {
      largeResource.loadAsBytes
    }
  }

  private class TestResource(contents: Array[Byte], val size: Option[Long]) extends Resource {
    override def name: String = "largefile"
    override def path: String = "path"
    override def exists: Boolean = true
    override def modificationTime: Option[Instant] = None
    override def inputStream: InputStream = new ByteArrayInputStream(contents)
  }

}
