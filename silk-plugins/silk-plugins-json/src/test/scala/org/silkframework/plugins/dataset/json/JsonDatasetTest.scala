package org.silkframework.plugins.dataset.json

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.resource.{ResourceTooLargeException, WritableResource}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.time.Instant

class JsonDatasetTest extends FlatSpec with Matchers with TestUserContextTrait {

  behavior of "JSON dataset"

  it should "not load large files into memory" in {
    val resource = new WritableResource {
      override def name: String = "largeResource"
      override def path: String = "path"
      override def size: Option[Long] = Some(1000000000L)

      override def createOutputStream(append: Boolean): OutputStream = new ByteArrayOutputStream()
      override def delete(): Unit = {}
      override def exists: Boolean = true
      override def modificationTime: Option[Instant] = None
      override def inputStream: InputStream = new ByteArrayInputStream(Array.emptyByteArray)
    }

    val dataset = JsonDataset(resource, streaming = false)
    an[ResourceTooLargeException] should be thrownBy dataset.source
  }

}
