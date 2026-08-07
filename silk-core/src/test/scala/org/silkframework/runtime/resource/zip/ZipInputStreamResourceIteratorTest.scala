package org.silkframework.runtime.resource.zip

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.runtime.resource.{CompressedFileResource, CompressedInMemoryResource, FileResource, Resource}

import java.io.File
import java.nio.file.Files
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}

class ZipInputStreamResourceIteratorTest extends AnyFlatSpec with Matchers {

  behavior of "ZipInputStreamResourceIterator"

  private val threshold = 64 * 1000

  private val smallContent = "x" * 100
  // Larger than the in-memory threshold, but highly compressible, so the zip entry itself stays tiny
  private val largeContent = "y" * (threshold * 3)

  /** Writes a zip via ZipOutputStream, i.e. in streaming mode, which is how our own exports are produced. */
  private def withStreamedZip(entries: Seq[(String, String)])(test: FileResource => Unit): Unit = {
    val zipFile = File.createTempFile("zipIteratorTest", ".zip")
    try {
      val zipOut = new ZipOutputStream(Files.newOutputStream(zipFile.toPath))
      try {
        for ((name, content) <- entries) {
          zipOut.putNextEntry(new ZipEntry(name))
          zipOut.write(content.getBytes("UTF-8"))
          zipOut.closeEntry()
        }
      } finally {
        zipOut.close()
      }
      test(FileResource(zipFile))
    } finally {
      zipFile.delete()
    }
  }

  /** Reads each resource while the iterator is positioned on it, since the previous one is deleted on advance. */
  private def collect(zip: FileResource): Seq[(Resource, String)] = {
    val iterator = ZipInputStreamResourceIterator(zip, "").iterateReadOnceResources(".*".r)
    try {
      iterator.map(resource => (resource, resource.loadAsString())).toList
    } finally {
      iterator.close()
    }
  }

  it should "keep small entries in memory" in {
    withStreamedZip(Seq("small.txt" -> smallContent)) { zip =>
      val results = collect(zip)
      results.map(_._2) mustBe Seq(smallContent)
      results.head._1 mustBe a[CompressedInMemoryResource]
    }
  }

  it should "spill entries larger than the threshold to a temp file even when the compressed size is unknown" in {
    withStreamedZip(Seq("large.txt" -> largeContent)) { zip =>
      val results = collect(zip)
      results.map(_._2) mustBe Seq(largeContent)
      results.head._1 mustBe a[CompressedFileResource]
    }
  }

  it should "read mixed entries completely and pick the storage per entry" in {
    val entries = Seq("small.txt" -> smallContent, "large.txt" -> largeContent, "small2.txt" -> smallContent)
    withStreamedZip(entries) { zip =>
      val results = collect(zip)
      // Content of every entry must survive the read-ahead unchanged, in order
      results.map(_._2) mustBe entries.map(_._2)
      results.map(_._1.getClass) mustBe Seq(
        classOf[CompressedInMemoryResource], classOf[CompressedFileResource], classOf[CompressedInMemoryResource])
    }
  }
}
