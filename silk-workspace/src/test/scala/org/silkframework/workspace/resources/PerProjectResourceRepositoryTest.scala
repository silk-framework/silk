package org.silkframework.workspace.resources

abstract class PerProjectResourceRepositoryTest extends ResourceRepositoryTest {

  it should "separate files from different projects" in {
    repository.sharedResources mustBe false

    val resourceName = "resource"
    val AManager = repository.get("projectA")
    val BManager = repository.get("projectB")
    val resourceA = AManager.get(resourceName)
    val resourceB = BManager.get(resourceName)

    resourceA.exists mustBe false
    resourceB.exists mustBe false

    resourceA.writeString("A")

    resourceA.exists mustBe true
    resourceB.exists mustBe false

    resourceB.writeString("B")

    resourceA.exists mustBe true
    resourceB.exists mustBe true

    resourceA.loadAsString mustBe "A"
    resourceB.loadAsString mustBe "B"

    AManager.delete(resourceName)
    resourceA.exists mustBe false
    resourceB.exists mustBe true
    resourceB.delete()
    resourceB.exists mustBe false
  }

  it should "delete all project resources if requested" in {
    val resourceA1 = repository.get("projectA").get("resource")
    val resourceA2 = repository.get("projectA").child("folder").get("resource")
    val resourceB = repository.get("projectB").get("resource")

    resourceA1.writeString("A1")
    resourceA2.writeString("A2")
    resourceB.writeString("B")

    resourceA1.exists mustBe true
    resourceA2.exists mustBe true
    resourceB.exists mustBe true

    repository.removeProjectResources("projectA")

    resourceA1.exists mustBe false
    resourceA2.exists mustBe false
    resourceB.exists mustBe true

    resourceB.delete()
  }

}
