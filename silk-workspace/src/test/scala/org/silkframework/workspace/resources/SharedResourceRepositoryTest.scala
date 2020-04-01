package org.silkframework.workspace.resources

abstract class SharedResourceRepositoryTest extends ResourceRepositoryTest {

  it should "share files from different projects" in {
    val resourceA = repository.get("projectA").get("resource")
    val resourceB = repository.get("projectB").get("resource")

    resourceA.exists mustBe false
    resourceB.exists mustBe false

    resourceA.writeString("A")

    resourceA.loadAsString mustBe "A"
    resourceB.loadAsString mustBe "A"
    resourceA.exists mustBe true
    resourceB.exists mustBe true

    resourceA.delete()
    resourceA.exists mustBe false
    resourceB.exists mustBe false
  }

  it should "not delete shared files" in {
    val resource = repository.get("projectA").get("resource")
    resource.writeString("A")
    resource.exists mustBe true

    repository.removeProjectResources("projectA")
    resource.exists mustBe true
  }

}
