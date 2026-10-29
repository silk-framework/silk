package org.silkframework.runtime.resource

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.io.{File, IOException, InputStream}
import java.net.{ConnectException, URL, URLConnection, URLStreamHandler, UnknownHostException}
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
    UrlResource(failingUrl(new UnknownHostException("unknown.host.invalid"))).exists mustBe false
  }

  it should "report a resource as not existing if the connection is refused" in {
    UrlResource(failingUrl(new ConnectException("Connection refused"))).exists mustBe false
  }

  /** A URL whose connection fails with the given error, so connection failures can be tested without the network. */
  private def failingUrl(error: IOException): URL = {
    val handler = new URLStreamHandler {
      override def openConnection(u: URL): URLConnection = new URLConnection(u) {
        override def connect(): Unit = throw error
        override def getInputStream: InputStream = throw error
      }
    }
    new URL("http", "example.org", -1, "/resource", handler)
  }
}
