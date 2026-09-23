package org.silkframework.runtime.resource

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Instant

class InMemoryResourceManagerTest extends AnyFlatSpec with Matchers {

  "InMemoryResourceManager" should "allow retrieval of stored values" in {
    val res = InMemoryResourceManager()
    res.get("name").writeString("TESTDATA")
    res.get("name").loadAsString() should be ("TESTDATA")
  }

  it should "allow overiting values" in {
    val res = InMemoryResourceManager()
    res.get("name").writeString("TESTDATA")
    res.get("name").writeString("Updated Data")
    res.get("name").loadAsString() should be ("Updated Data")
  }

  it should "allow nested child resources" in {
    val res = InMemoryResourceManager()
    res.get("name").writeString("Parent Data")
    res.child("childName").get("name").writeString("Child Data")
    res.child("childName").get("name").loadAsString() should be ("Child Data")
    res.child("childName").parent.get.get("name").loadAsString() should be ("Parent Data")
  }

  it should "return resources that are shared" in {
    val res = InMemoryResourceManager()
    val res1 = res.get("name")
    val res2 = res.get("name")
    res1.writeString("content")
    res2.loadAsString() shouldBe "content"
  }

  it should "not list a child folder that has only been accessed" in {
    val res = InMemoryResourceManager()
    res.child("childName")
    res.listChildren shouldBe empty
  }

  it should "list a child folder once a resource has been written into it or one of its descendants" in {
    val res = InMemoryResourceManager()
    res.child("childName").child("grandChild").get("name").writeString("data")
    res.listChildren shouldBe List("childName")
    res.child("childName").listChildren shouldBe List("grandChild")
  }

  it should "keep listing a child folder after all of its resources have been deleted" in {
    val res = InMemoryResourceManager()
    res.child("childName").get("name").writeString("data")
    res.child("childName").get("name").delete()
    res.listChildren shouldBe List("childName")
  }

  it should "not list a deleted child folder until something is written into it again" in {
    val res = InMemoryResourceManager()
    res.child("childName").get("name").writeString("data")
    res.delete("childName")
    res.child("childName").list shouldBe empty
    res.listChildren shouldBe empty
    res.child("childName").get("name").writeString("data")
    res.listChildren shouldBe List("childName")
  }

  it should "keep a child folder when a resource of the same name is deleted" in {
    val res = InMemoryResourceManager()
    res.get("name").writeString("resource")
    res.child("name").get("name").writeString("child")
    res.get("name").delete()
    res.exists("name") shouldBe false
    res.listChildren shouldBe List("name")
    res.child("name").get("name").loadAsString() shouldBe "child"
  }

  it should "ignore a second close of an output stream" in {
    val res = InMemoryResourceManager()
    res.get("name").writeString("first")
    val outputStream = res.get("name").createOutputStream(append = true)
    outputStream.write("second".getBytes)
    outputStream.close()
    outputStream.close()
    res.get("name").loadAsString() shouldBe "firstsecond"
  }

  it should "report the modification time of written resources" in {
    val res = InMemoryResourceManager()
    val before = Instant.now()
    res.get("name").modificationTime shouldBe None
    res.get("name").writeString("data")
    val modificationTime = res.get("name").modificationTime
    modificationTime shouldBe defined
    modificationTime.get.isBefore(before) shouldBe false
  }

}
