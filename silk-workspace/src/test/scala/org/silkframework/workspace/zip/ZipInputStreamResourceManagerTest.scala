package org.silkframework.workspace.zip

import java.io.File

import org.scalatest.{FlatSpec, MustMatchers}

class ZipInputStreamResourceManagerTest extends FlatSpec with MustMatchers {

  behavior of "zip resource managers"

  private val tempDir = "temp/"

  it should "zip and unzip files" in {
    val zipFile = File.createTempFile(tempDir + "outZip", ".zip")
    val outputResourceManager = new ZipOutputStreamResourceManager(zipFile, "", true)

    val pseudoFiles = Map("first"-> "a,b,c", "second"-> "d,e,f", "third"->"g,h,i")
    pseudoFiles.foreach(fileContent => {
      val next = outputResourceManager.get(fileContent._1)
      next.writeString(fileContent._2)
    })
    outputResourceManager.close()

    val zipIn = new ZipInputStreamResourceManager(zipFile.toPath)
    pseudoFiles.foreach(file =>{
      val resource = zipIn.get(file._1)
      resource.loadLines.head mustBe file._2
    })
    zipFile.delete()
  }

  it should "deal with hierarchical read and write requests" in {
    val zipFile = File.createTempFile(tempDir + "outZip", ".zip")
    val manager = new ZipOutputStreamResourceManager(zipFile, "", true)
    val res = manager.getInPath("someDir/another/test.txt")
    res.writeString("some tests")
    manager.close()

    val readManager = new ZipInputStreamResourceManager(zipFile.toPath)
    val child1 = readManager.child("someDir")
    val child2 = child1.child("another")
    child2.get("test.txt").loadLines.head mustBe "some tests"
    readManager.listChildren mustBe List("someDir")
    readManager.child("someDir") == child1 mustBe true
  }

  //TODO path canonical check not yet implemented
  ignore should "not allow access below its base path" in {
    val zipFile = File.createTempFile(tempDir + "outZip", ".zip")
    val manager = new ZipOutputStreamResourceManager(zipFile, "", true)
    manager.get("someDir/../allowedAccess")
    manager.get(s"someDir/../../${zipFile.getName}/allowedAccess")
    intercept[IllegalArgumentException] {
      manager.get("../../etc/passwd")
    }
    intercept[IllegalArgumentException] {
      manager.get("somePath/morePath/./../../../etc/passwd")
    }
  }
}
