package org.silkframework.runtime.resource

import java.io.{ByteArrayInputStream, InputStream}
import java.time.Instant
import java.time.temporal.ChronoUnit

import org.scalatest.{FlatSpec, Matchers}

class ResourceCacheTest extends FlatSpec with Matchers {

  behavior of "ResourceCache"

  it should "load the value on first cache reading" in {
    val resource = new TestResource()
    val cache = new TestResourceCache(resource, updateTimeout = 5000L)
    resource.value = "test value"

    // Read value twice and make sure it has only been read once
    cache.value shouldBe "test value"
    cache.value shouldBe "test value"
    resource.readCounter shouldBe 1
  }

  it should "update the value only when the modification time changed" in {
    val resource = new TestResource()
    val cache = new TestResourceCache(resource, updateTimeout = 1000L)

    // Set initial resource value and access it
    resource.value = "test value"
    resource.modificationTime = Some(Instant.now())
    cache.value shouldBe "test value"
    resource.readCounter shouldBe 1

    // Update resource value and modification date
    resource.value = "updated value"
    resource.modificationTime = Some(resource.modificationTime.get.plus(1, ChronoUnit.MINUTES))

    // Caches returns the previous value while the timeout has not been reached
    cache.value shouldBe "test value"
    resource.readCounter shouldBe 1

    // Let timeout pass and check that the updated value is returned
    Thread.sleep(1001L)
    cache.value shouldBe "updated value"
    resource.readCounter shouldBe 2

    // Accessing the value without updating the modification time does not load again
    Thread.sleep(1001L)
    cache.value shouldBe "updated value"
    resource.readCounter shouldBe 2
  }

  class TestResource() extends Resource {

    override def name: String = "TestResource"

    var value = "initial value"

    var modificationTime: Option[Instant] = Some(Instant.now)

    var readCounter = 0

    override def inputStream: InputStream = {
      readCounter += 1
      new ByteArrayInputStream(value.getBytes("UTF8"))
    }

    override def path: String = name
    override def exists: Boolean = true
    override def size: Option[Long] = None
  }

  class TestResourceCache(resource: Resource, updateTimeout: Long) extends ResourceCache[String](resource, updateTimeout) {
    override protected def load(): String = {
      resource.loadAsString
    }
  }

}
