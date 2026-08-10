package org.silkframework.runtime.resource

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.io.File
import java.net.URL
import java.nio.file.Files

class UrlResourceTest extends AnyFlatSpec with Matchers {

  behavior of "UrlResource"

  it should "read a resource that exists" in {
    val file = File.createTempFile("urlResourceTest", ".txt")
    try {
      Files.writeString(file.toPath, "content")
      val resource = UrlResource(file.toURI.toURL)
      resource.exists mustBe true
      resource.loadAsString() mustBe "content"
    } finally {
      file.delete()
    }
  }

  it should "report a missing resource as not existing" in {
    val file = new File(System.getProperty("java.io.tmpdir"), "urlResourceTest-does-not-exist.txt")
    UrlResource(file.toURI.toURL).exists mustBe false
  }

  it should "report a resource on an unknown host as not existing" in {
    UrlResource(new URL("http://unknown.host.invalid/resource")).exists mustBe false
  }

  it should "report a resource as not existing if the connection is refused" in {
    // Port 1 is not served, so the connection is refused instead of raising a FileNotFoundException
    UrlResource(new URL("http://localhost:1/resource"), connectTimeout = Some(2000)).exists mustBe false
  }
}
