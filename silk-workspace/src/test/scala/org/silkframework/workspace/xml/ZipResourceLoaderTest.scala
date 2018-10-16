package org.silkframework.workspace.xml

import java.util.zip.ZipFile

import org.scalatest.{BeforeAndAfterAll, FlatSpec, ShouldMatchers}

class ZipResourceLoaderTest extends FlatSpec with ShouldMatchers with BeforeAndAfterAll {

  behavior of "ZipResourceLoader"

  private val zip = new ZipFile(getClass.getResource("example.zip").getFile)
  private val root = ZipResourceLoader(zip)

  it should "read all files and folders at the root" in {
    root.listChildren shouldBe List("rootFolder")
    root.list shouldBe List("rootFile.txt")
    root.get("rootFile.txt").loadAsString shouldBe "root"
  }

  it should "read files and folders at different base paths" in {
    val child = root.child("rootFolder")
    child.listChildren shouldBe List("childFolder")
    child.list shouldBe List("childFile.txt")
    child.get("childFile.txt").loadAsString shouldBe "child"
  }

  override def afterAll(): Unit = {
    zip.close()
  }

}
