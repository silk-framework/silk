package org.silkframework.runtime.resource.zip

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.io.File
import java.nio.file.Files
import java.util.zip.{ZipEntry, ZipOutputStream}

class ZipFileResourceLoaderTest extends AnyFlatSpec with Matchers {

  behavior of "ZipFileResourceLoader"

  // 'proj' and 'proj2' share a name prefix, as do 'proj' and the root entry 'projx'
  private val entries = Seq(
    "root.txt",
    "projx",
    "proj/config.xml",
    "proj/sub/nested.csv",
    "proj2/config.xml"
  )

  private def withZip(test: File => Unit): Unit = {
    val zipFile = File.createTempFile("zipFileLoaderTest", ".zip")
    try {
      val zipOut = new ZipOutputStream(Files.newOutputStream(zipFile.toPath))
      try {
        for (name <- entries) {
          zipOut.putNextEntry(new ZipEntry(name))
          zipOut.write("content".getBytes("UTF-8"))
          zipOut.closeEntry()
        }
      } finally {
        zipOut.close()
      }
      test(zipFile)
    } finally {
      zipFile.delete()
    }
  }

  /** Runs a test on a loader and makes sure the zip file handle is released again. */
  private def withLoader(zipFile: File, basePath: String)(test: ZipFileResourceLoader => Unit): Unit = {
    val loader = ZipFileResourceLoader(zipFile, basePath)
    try {
      test(loader)
    } finally {
      loader.zip.close()
    }
  }

  it should "list files and children at the zip root" in {
    withZip { zipFile =>
      withLoader(zipFile, "") { loader =>
        loader.list.sorted mustBe List("projx", "root.txt")
        loader.listChildren.sorted mustBe List("proj", "proj2")
      }
    }
  }

  it should "not list a root entry that shares the base path as a name prefix" in {
    withZip { zipFile =>
      withLoader(zipFile, "proj") { loader =>
        // 'projx' starts with 'proj' but is not below it
        loader.list mustBe List("config.xml")
      }
    }
  }

  it should "not report a prefix-sharing sibling directory as a child" in {
    withZip { zipFile =>
      withLoader(zipFile, "proj") { loader =>
        // 'proj2/config.xml' must not surface 'proj2' as a child of 'proj'
        loader.listChildren mustBe List("sub")
      }
    }
  }

  it should "list the content of a nested base path" in {
    withZip { zipFile =>
      withLoader(zipFile, "proj/sub") { loader =>
        loader.list mustBe List("nested.csv")
        loader.listChildren mustBe empty
      }
    }
  }

  it should "keep listings of the prefix-sharing sibling itself intact" in {
    withZip { zipFile =>
      withLoader(zipFile, "proj2") { loader =>
        loader.list mustBe List("config.xml")
        loader.listChildren mustBe empty
      }
    }
  }
}
