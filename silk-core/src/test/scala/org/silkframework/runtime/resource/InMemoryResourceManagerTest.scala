package org.silkframework.runtime.resource

import org.scalatest.{FlatSpec, Matchers}

class InMemoryResourceManagerTest extends FlatSpec with Matchers {

  "InMemoryResourceManager" should "allow retrieval of stored values" in {
    val res = InMemoryResourceManager()
    res.get("name").writeString("TESTDATA")
    res.get("name").loadAsString should be ("TESTDATA")
  }

  it should "allow overiting values" in {
    val res = InMemoryResourceManager()
    res.get("name").writeString("TESTDATA")
    res.get("name").writeString("Updated Data")
    res.get("name").loadAsString should be ("Updated Data")
  }

  it should "allow nested child resources" in {
    val res = InMemoryResourceManager()
    res.get("name").writeString("Parent Data")
    res.child("childName").get("name").writeString("Child Data")
    res.child("childName").get("name").loadAsString should be ("Child Data")
    res.child("childName").parent.get.get("name").loadAsString should be ("Parent Data")
  }

  it should "return resources that are shared" in {
    val res = InMemoryResourceManager()
    val res1 = res.get("name")
    val res2 = res.get("name")
    res1.writeString("content")
    res2.loadAsString shouldBe "content"
  }

}
