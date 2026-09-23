package org.silkframework.runtime.resource

import org.scalatest.flatspec.AnyFlatSpec

class InMemoryResourceManagerTest extends AnyFlatSpec with ResourceManagerTestTrait {

  override protected def createResourceManager(): ResourceManager = InMemoryResourceManager()

  behavior of "In-memory resource manager"

  // Like on the file system, an emptied folder is still there
  it should "keep listing a child folder after all of its resources have been deleted" in {
    val res = InMemoryResourceManager()
    res.child("childName").get("name").writeString("data")
    res.child("childName").get("name").delete()
    res.listChildren shouldBe List("childName")
  }

  // Unlike the file system, memory can hold a resource and a folder of the same name
  it should "keep a child folder when a resource of the same name is deleted" in {
    val res = InMemoryResourceManager()
    res.get("name").writeString("resource")
    res.child("name").get("name").writeString("child")
    res.get("name").delete()
    res.exists("name") shouldBe false
    res.listChildren shouldBe List("name")
    res.child("name").get("name").loadAsString() shouldBe "child"
  }

  // Like a file write recreates deleted directories
  it should "recreate a deleted folder when writing through a handle held across the delete" in {
    val res = InMemoryResourceManager()
    val child = res.child("parent").child("child")
    res.child("parent").delete("child")
    child.get("name").writeString("data")
    res.listChildren shouldBe List("parent")
    res.child("parent").listChildren shouldBe List("child")
    res.child("parent").child("child").get("name").loadAsString() shouldBe "data"
  }

  it should "recreate all deleted ancestors when writing through a handle held across the delete" in {
    val res = InMemoryResourceManager()
    val child = res.child("parent").child("child")
    res.child("parent").child("child").get("name").writeString("old")
    res.delete("parent")
    res.listChildren shouldBe empty
    child.get("name").writeString("new")
    res.child("parent").child("child").get("name").loadAsString() shouldBe "new"
  }

  it should "not attach a stale folder handle over a folder created after the delete" in {
    val res = InMemoryResourceManager()
    val stale = res.child("parent")
    res.delete("parent")
    res.child("parent").get("name").writeString("fresh")
    stale.get("name").writeString("stale")
    res.child("parent").get("name").loadAsString() shouldBe "fresh"
  }

  it should "not list the ancestors of a stale folder handle that has been superseded" in {
    val res = InMemoryResourceManager()
    val stale = res.child("parent").child("child")
    res.child("parent").delete("child")
    res.child("parent").child("child")
    stale.get("name").writeString("stale")
    res.listChildren shouldBe empty
  }

}
