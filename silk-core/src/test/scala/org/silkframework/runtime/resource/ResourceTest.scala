package org.silkframework.runtime.resource

import java.io.{ByteArrayInputStream, IOException, InputStream}
import java.time.Instant

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ResourceTest extends AnyFlatSpec with Matchers {

  behavior of "Resource"

  it should "not allow large sources to be loaded into memory" in {
    val smallResource = new TestResource("small".getBytes, size = Some(10))
    val largeResource = new TestResource("large".getBytes, size = Some(1000000000))

    smallResource.loadAsString() mustBe "small"
    intercept[IOException] {
      largeResource.loadAsString()
    }
    intercept[IOException] {
      largeResource.loadAsBytes
    }
  }

  behavior of "Resource.loadAsStringCapped"

  it should "cap the loaded string and flag truncation, regardless of resource size" in {
    // The declared size is far over the in-memory limit: capped reads must not reject it.
    val largeResource = new TestResource("0123456789".getBytes, size = Some(1000000000))
    largeResource.loadAsStringCapped(4) mustBe CappedString("0123", truncated = true)
    largeResource.loadAsStringCapped(10) mustBe CappedString("0123456789", truncated = false)
    largeResource.loadAsStringCapped(20) mustBe CappedString("0123456789", truncated = false)
    largeResource.loadAsStringCapped(0) mustBe CappedString("", truncated = true)
    new TestResource(Array.emptyByteArray, size = Some(0)).loadAsStringCapped(0) mustBe CappedString("", truncated = false)
  }

  behavior of "Resource.relativePath"

  it should "return the '/'-separated path relative to the resource manager base path" in {
    val mgr = InMemoryResourceManager()
    val resource = mgr.getInPath("a/nested/path")
    resource.relativePath(mgr) mustBe "a/nested/path"
    resource.relativePath(mgr.child("a")) mustBe "nested/path"
  }

  it should "reject resources whose path shares the base path only as a string prefix" in {
    val mgr = InMemoryResourceManager()
    val sibling = mgr.getInPath("proj2/file.csv")
    intercept[IllegalArgumentException] {
      sibling.relativePath(mgr.child("proj"))
    }
  }

  it should "normalize Windows path separators to '/'" in {
    def windowsResource(resourcePath: String) = new TestResource("test".getBytes, size = Some(4)) {
      override def name: String = "file.csv"
      override def path: String = resourcePath
    }
    val mgr = new InMemoryResourceManagerBase("C:\\data\\proj")
    windowsResource("C:\\data\\proj\\dir\\file.csv").relativePath(mgr, separatorChar = '\\') mustBe "dir/file.csv"
    intercept[IllegalArgumentException] {
      windowsResource("C:\\data\\proj2\\file.csv").relativePath(mgr, separatorChar = '\\')
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
