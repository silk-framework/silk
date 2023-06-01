package org.silkframework.workspace.zip

import java.io.File
import java.util.zip.ZipFile

import org.silkframework.runtime.resource.FileResource
import org.silkframework.runtime.resource.zip.{ZipFileResourceLoader, ZipOutputStreamResourceManager}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ZipInputStreamResourceManagerTest extends AnyFlatSpec with Matchers {

  behavior of "zip resource managers"

  private val tempDir = "temp/"

  it should "zip and unzip files" in {
    val zipFile = FileResource(File.createTempFile(tempDir + "outZip", ".zip"))
    val outputResourceManager = new ZipOutputStreamResourceManager(zipFile.file, "", true)

    val pseudoFiles = Map("first"-> "a,b,c", "second"-> "d,e,f", "third"->"g,h,i")
    pseudoFiles.foreach(fileContent => {
      val next = outputResourceManager.get(fileContent._1)
      next.writeString(fileContent._2)
    })
    outputResourceManager.close()

    val zipIn = ZipFileResourceLoader(new ZipFile(zipFile.file))
    pseudoFiles.foreach(file =>{
      val resource = zipIn.get(file._1)
      resource.loadLines().head mustBe file._2
    })
    zipFile.delete()
  }

  it should "deal with hierarchical read and write requests" in {
    val zipFile = FileResource(File.createTempFile(tempDir + "outZip", ".zip"))
    val manager = new ZipOutputStreamResourceManager(zipFile.file, "", true)
    val res = manager.getInPath("someDir/another/test.txt")
    res.writeString("some tests")
    manager.close()

    val readManager = ZipFileResourceLoader(zipFile.file, "")
    val child1 = readManager.child("someDir")
    val child2 = child1.child("another")
    child2.get("test.txt").loadLines().head mustBe "some tests"
    readManager.listChildren mustBe List("someDir")
    readManager.child("someDir").basePath mustBe child1.basePath
    readManager.child("someDir").list mustBe child1.list
  }

  it should "not allow access below its base path" in {
    val zipFile = File.createTempFile(tempDir + "outZip", ".zip")
    val manager = new ZipOutputStreamResourceManager(zipFile, "", true)
    manager.get("someDir/../allowedAccess")
    intercept[IllegalArgumentException] {
      manager.get("../../etc/passwd")
    }
    intercept[IllegalArgumentException] {
      manager.get("somePath/morePath/./../../../etc/passwd")
    }

    val managerWithBaseDir = new ZipOutputStreamResourceManager(zipFile, "base/Path", true)
    managerWithBaseDir.get("someDir/../allowedAccess")
    intercept[IllegalArgumentException] {
      managerWithBaseDir.get("../../etc/passwd")
    }
  }

  it should "work with all allowed paths" in {
    val zipFile = File.createTempFile(tempDir + "outZip", ".zip")
    val managerWithBaseDir = new ZipOutputStreamResourceManager(zipFile, "base/Path", true)
    managerWithBaseDir.get("dir/path with spaces")
    managerWithBaseDir.get("dir/path$with#special&characters")
  }
}
