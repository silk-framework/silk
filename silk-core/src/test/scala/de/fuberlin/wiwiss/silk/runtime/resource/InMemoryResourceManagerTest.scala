package de.fuberlin.wiwiss.silk.runtime.resource

import org.scalatest.{Matchers, FlatSpec}

class InMemoryResourceManagerTest extends FlatSpec with Matchers {

  "InMemoryResourceManager" should "allow retrieval of stored values" in {
    val res = InMemoryResourceManager()
    res.put("name", "TESTDATA")
    res.get("name").loadAsString should be ("TESTDATA")
  }

  it should "allow overiting values" in {
    val res = InMemoryResourceManager()
    res.put("name", "TESTDATA")
    res.put("name", "Updated Data")
    res.get("name").loadAsString should be ("Updated Data")
  }

  it should "allow nested child resources" in {
    val res = InMemoryResourceManager()
    res.put("childName", "Parent Data")
    res.child("childName").put("name", "Child Data")
    res.child("childName").get("name").loadAsString should be ("Child Data")
    res.child("childName").parent.get.get("childName").loadAsString should be ("Parent Data")
  }

}
