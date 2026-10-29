package org.silkframework.runtime.resource.zip

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.runtime.resource.{FileResource, ResourceNotFoundException, WritableResource}

import java.io.{File, FilterInputStream, InputStream, OutputStream}
import java.nio.file.Files
import java.time.Instant
import java.util.zip.{ZipEntry, ZipOutputStream}

class ZipResourceTest extends AnyFlatSpec with Matchers {

  behavior of "ZipResource"

  /** Delegating resource that counts how many input streams have been opened and how many of them were closed again. */
  private class StreamCountingResource(resource: WritableResource) extends WritableResource {
    var opened = 0
    var closed = 0

    override def inputStream: InputStream = {
      opened += 1
      new FilterInputStream(resource.inputStream) {
        override def close(): Unit = {
          closed += 1
          super.close()
        }
      }
    }

    def leakedStreams: Int = opened - closed

    override def name: String = resource.name
    override def path: String = resource.path
    override def exists: Boolean = resource.exists
    override def size: Option[Long] = resource.size
    override def modificationTime: Option[Instant] = resource.modificationTime
    override def createOutputStream(append: Boolean): OutputStream = resource.createOutputStream(append)
    override def delete(): Unit = resource.delete()
  }

  /** Creates a zip file holding a single entry 'existing.txt' and runs the test against a stream counting resource. */
  private def withZip(test: StreamCountingResource => Unit): Unit = {
    val zipFile = File.createTempFile("zipResourceTest", ".zip")
    try {
      val zipOut = new ZipOutputStream(Files.newOutputStream(zipFile.toPath))
      try {
        zipOut.putNextEntry(new ZipEntry("existing.txt"))
        zipOut.write("content".getBytes("UTF-8"))
        zipOut.closeEntry()
      } finally {
        zipOut.close()
      }
      test(new StreamCountingResource(FileResource(zipFile)))
    } finally {
      zipFile.delete()
    }
  }

  it should "read an existing entry" in {
    withZip { zip =>
      val resource = new ZipResource(zip, "existing.txt")
      resource.loadAsString() mustBe "content"
      // The stream handed to the caller is closed by loadAsString
      zip.leakedStreams mustBe 0
    }
  }

  it should "not leak the underlying stream if the entry does not exist" in {
    withZip { zip =>
      val resource = new ZipResource(zip, "missing.txt")
      intercept[ResourceNotFoundException] {
        resource.inputStream
      }
      zip.opened must be > 0
      zip.leakedStreams mustBe 0
    }
  }

  it should "not leak the underlying stream if the zip file is corrupt" in {
    val brokenZip = File.createTempFile("brokenZipResourceTest", ".zip")
    try {
      Files.write(brokenZip.toPath, "this is not a zip file".getBytes("UTF-8"))
      val zip = new StreamCountingResource(FileResource(brokenZip))
      val resource = new ZipResource(zip, "anything.txt")
      // Either the scan fails or the entry is simply not found, but in both cases nothing may leak
      intercept[Exception] {
        resource.inputStream
      }
      zip.leakedStreams mustBe 0
    } finally {
      brokenZip.delete()
    }
  }

  it should "report the missing entry name without wrapping it in an Option" in {
    withZip { zip =>
      val ex = intercept[ResourceNotFoundException] {
        new ZipResource(zip, "missing.txt").inputStream
      }
      ex.getMessage must include("missing.txt")
      ex.getMessage mustNot include("Some(")
    }
  }
}
