package org.silkframework.runtime.resource

import java.util.zip.ZipInputStream

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.silkframework.runtime.resource.zip.ZipResourceLoader

class ZipResourceLoaderTest extends FlatSpec with Matchers with BeforeAndAfterAll {

  behavior of "ZipResourceLoader"

  private val root = ZipResourceLoader(() => new ZipInputStream(getClass.getResource("example.zip").openStream()))

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

}
