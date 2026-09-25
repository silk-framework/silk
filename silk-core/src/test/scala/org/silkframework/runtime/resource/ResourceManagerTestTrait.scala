package org.silkframework.runtime.resource

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Instant

/**
  * Contract that every resource manager with real storage has to fulfil. The file system defines the semantics.
  * Deriving suites implement [[createResourceManager]] and add their storage-specific tests.
  */
trait ResourceManagerTestTrait extends AnyFlatSpec with Matchers {

  /** Creates a fresh, empty resource manager. Called once per test. */
  protected def createResourceManager(): ResourceManager

  /** False for storages that cannot append to an existing resource, such as S3. The append tests are then canceled. */
  protected def supportsAppend: Boolean = true

  behavior of "Resource manager"

  it should "store and read back a resource" in {
    val rm = createResourceManager()
    rm.get("name").writeString("data")
    rm.get("name").loadAsString() shouldBe "data"
  }

  it should "overwrite a resource" in {
    val rm = createResourceManager()
    rm.get("name").writeString("data")
    rm.get("name").writeString("updated")
    rm.get("name").loadAsString() shouldBe "updated"
  }

  it should "append to a resource" in {
    assume(supportsAppend)
    val rm = createResourceManager()
    rm.get("name").writeString("first")
    rm.get("name").writeString("second", append = true)
    rm.get("name").loadAsString() shouldBe "firstsecond"
  }

  it should "write and read raw bytes" in {
    val rm = createResourceManager()
    val bytes = Array[Byte](1, 2, 3)
    rm.get("name").writeBytes(bytes)
    rm.get("name").loadAsBytes shouldBe bytes
    rm.get("name").size shouldBe Some(3L)
  }

  it should "write through an output stream" in {
    val rm = createResourceManager()
    val outputStream = rm.get("name").createOutputStream()
    outputStream.write("data".getBytes)
    outputStream.close()
    rm.get("name").loadAsString() shouldBe "data"
  }

  it should "share the storage between handles of the same resource" in {
    val rm = createResourceManager()
    val handle1 = rm.get("name")
    val handle2 = rm.get("name")
    handle1.writeString("data")
    handle2.loadAsString() shouldBe "data"
  }

  it should "report existence and list resources" in {
    val rm = createResourceManager()
    rm.get("name").exists shouldBe false
    rm.exists("name") shouldBe false
    rm.list shouldBe empty
    rm.get("name").writeString("data")
    rm.get("name").exists shouldBe true
    rm.exists("name") shouldBe true
    rm.list shouldBe List("name")
  }

  it should "throw when a resource must exist but does not" in {
    val rm = createResourceManager()
    intercept[ResourceNotFoundException] {
      rm.get("missing", mustExist = true)
    }
    rm.get("missing", mustExist = false).exists shouldBe false
  }

  it should "delete a resource" in {
    val rm = createResourceManager()
    rm.get("name").writeString("data")
    rm.get("name").delete()
    rm.get("name").exists shouldBe false
    rm.list shouldBe empty
    rm.get("other").writeString("data")
    rm.delete("other")
    rm.get("other").exists shouldBe false
    rm.list shouldBe empty
  }

  it should "not list a child folder that has only been accessed" in {
    val rm = createResourceManager()
    rm.child("child")
    rm.listChildren shouldBe empty
  }

  it should "list a child folder once a resource has been written into it or one of its descendants" in {
    val rm = createResourceManager()
    rm.child("child").child("grandChild").get("name").writeString("data")
    rm.listChildren shouldBe List("child")
    rm.child("child").listChildren shouldBe List("grandChild")
    rm.child("child").child("grandChild").get("name").loadAsString() shouldBe "data"
  }

  it should "delete a child folder with all of its content" in {
    val rm = createResourceManager()
    rm.child("child").get("name").writeString("data")
    rm.child("child").child("grandChild").get("name").writeString("data")
    rm.delete("child")
    rm.listChildren shouldBe empty
    rm.child("child").list shouldBe empty
    rm.child("child").child("grandChild").get("name").exists shouldBe false
  }

  it should "not list a deleted child folder until something is written into it again" in {
    val rm = createResourceManager()
    rm.child("child").get("name").writeString("data")
    rm.delete("child")
    rm.child("child").list shouldBe empty
    rm.listChildren shouldBe empty
    rm.child("child").get("name").writeString("data")
    rm.listChildren shouldBe List("child")
  }

  it should "ignore a second close of an output stream" in {
    val rm = createResourceManager()
    val outputStream = rm.get("name").createOutputStream()
    outputStream.write("data".getBytes)
    outputStream.close()
    outputStream.close()
    rm.get("name").loadAsString() shouldBe "data"
  }

  it should "not append the data again on a second close of an appending output stream" in {
    assume(supportsAppend)
    val rm = createResourceManager()
    rm.get("name").writeString("first")
    val outputStream = rm.get("name").createOutputStream(append = true)
    outputStream.write("second".getBytes)
    outputStream.close()
    outputStream.close()
    rm.get("name").loadAsString() shouldBe "firstsecond"
  }

  it should "report the modification time of a written resource" in {
    val rm = createResourceManager()
    // Storages may round the time down to full seconds and file system clocks may lag behind Instant.now
    val tolerance = 2
    val before = Instant.now().minusSeconds(tolerance)
    rm.get("name").writeString("data")
    val after = Instant.now().plusSeconds(tolerance)
    val modificationTime = rm.get("name").modificationTime
    modificationTime shouldBe defined
    modificationTime.get.isBefore(before) shouldBe false
    modificationTime.get.isAfter(after) shouldBe false
  }

  it should "list resources recursively" in {
    val rm = createResourceManager()
    rm.get("name").writeString("data")
    rm.child("child").get("name").writeString("data")
    rm.child("child").child("grandChild").get("name").writeString("data")
    rm.listRecursive.sorted shouldBe List("child/grandChild/name", "child/name", "name")
  }

  it should "resolve resources by path" in {
    val rm = createResourceManager()
    rm.getInPath("child/grandChild/name").writeString("data")
    rm.child("child").child("grandChild").get("name").loadAsString() shouldBe "data"
    rm.getInPath("child/grandChild/name", mustExist = true).loadAsString() shouldBe "data"
    intercept[ResourceNotFoundException] {
      rm.getInPath("child/missing", mustExist = true)
    }
  }

  it should "navigate to the parent folder" in {
    val rm = createResourceManager()
    rm.get("name").writeString("data")
    val parent = rm.child("child").parent
    parent shouldBe defined
    parent.get.get("name").loadAsString() shouldBe "data"
  }

}
