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

  it should "fail on writes to a closed output stream" in {
    val res = InMemoryResourceManager()
    val os = res.get("name").createOutputStream()
    os.write(1)
    os.close()
    intercept[java.io.IOException](os.write(2))
    intercept[java.io.IOException](os.write(Array[Byte](2)))
    intercept[java.io.IOException](os.write(Array[Byte](2), 0, 1))
    res.get("name").size shouldBe Some(1L)
  }

}
