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

    // Read value multiple times and make sure it has only been read once
    for(_ <- 0 until 10) {
      cache.value shouldBe "test value"
    }
    resource.modificationCheckCounter shouldBe 1
    resource.readCounter shouldBe 1
  }

  it should "update the value only when the modification time changed" in {
    val resource = new TestResource()
    val cache = new TestResourceCache(resource, updateTimeout = 1000L)

    // Set initial resource value and access it
    resource.value = "test value"
    resource.modificationTimeValue = Instant.now()
    cache.value shouldBe "test value"
    resource.modificationCheckCounter shouldBe 1
    resource.readCounter shouldBe 1

    // Update resource value and modification date
    resource.value = "updated value"
    resource.modificationTimeValue = resource.modificationTimeValue.plus(1, ChronoUnit.MINUTES)

    // Caches returns the previous value while the timeout has not been reached
    cache.value shouldBe "test value"
    resource.modificationCheckCounter shouldBe 1
    resource.readCounter shouldBe 1

    // Let timeout pass and check that the updated value is returned
    Thread.sleep(1001L)
    cache.value shouldBe "updated value"
    resource.modificationCheckCounter shouldBe 2
    resource.readCounter shouldBe 2

    // Accessing the value without updating the modification time does not load again
    Thread.sleep(1001L)
    cache.value shouldBe "updated value"
    resource.modificationCheckCounter shouldBe 3
    resource.readCounter shouldBe 2
  }

  class TestResource() extends Resource {

    override def name: String = "TestResource"

    var value = "initial value"

    var modificationTimeValue: Instant = Instant.now

    // The number of times the resource has been read
    var readCounter = 0

    // The number of times the modification time has been checked
    var modificationCheckCounter = 0

    def modificationTime: Option[Instant] = {
      modificationCheckCounter += 1
      Some(modificationTimeValue)
    }

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
