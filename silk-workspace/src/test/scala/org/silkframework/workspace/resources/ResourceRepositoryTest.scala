package org.silkframework.workspace.resources

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.resource.ResourceNotFoundException

/**
  * Executes basic tests on a given resource repository.
  */
abstract class ResourceRepositoryTest extends FlatSpec with MustMatchers  {

  protected def repository: ResourceRepository

  private lazy val project = repository.get("myProject")

  private val testContent = "my resource"

  behavior of getClass.getSimpleName.stripSuffix("Test")

  it should "allow reading and writing resources" in {
    val testResource = project.get("resource")
    project.list mustBe Seq.empty
    testResource.exists mustBe false
    testResource.writeString(testContent)
    testResource.exists mustBe true
    project.list mustBe Seq("resource")
    testResource.name mustBe "resource"
    testResource.loadAsString mustBe testContent
    testResource.delete()
    testResource.exists mustBe false
  }

  it should "allow reading and writing nested resources" in {
    // Write some resources
    val testResource = project.child("child1").child("child2").get("resource")
    val testContent = "my resource"
    testResource.writeString(testContent)

    val testResource2 = project.get("rootResource")
    testResource2.writeString("rootResource")

    val testResource3 = project.child("child1").get("resource2")
    testResource3.writeString("resource2")

    // Test list children methods
    project.listChildren mustBe Seq("child1")
    project.child("child1").listChildren mustBe Seq("child2")
    project.listRecursive must contain theSameElementsAs Seq("rootResource", "child1/resource2", "child1/child2/resource")

    // Retrieve the resource manager again and do some navigation
    val testResourceRead = project.child("child1").parent.get.child("child1").child("child2").get("resource")
    testResourceRead.loadAsString mustBe testContent
    testResource.delete()
    testResource.exists mustBe false
    testResource2.delete()
    testResource3.delete()
  }

  it should "not allow to open resources that do not exist" in {
    intercept[ResourceNotFoundException] {
      project.get("nonExistingResource", mustExist = true)
    }
  }
}
