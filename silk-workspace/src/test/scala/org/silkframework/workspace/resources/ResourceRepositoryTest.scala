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
    val testResource = project.child("child1").child("child2").get("resource")
    val testContent = "my resource"
    testResource.writeString(testContent)

    project.listChildren mustBe Seq("child1")
    project.child("child1").listChildren mustBe Seq("child2")

    // Retrieve the resource manager again and do some navigation
    val testResourceRead = project.child("child1").parent.get.child("child1").child("child2").get("resource")
    testResourceRead.loadAsString mustBe testContent
    testResource.delete()
    testResource.exists mustBe false
  }

  it should "not allow to open resources that do not exist" in {
    intercept[ResourceNotFoundException] {
      project.get("nonExistingResource", mustExist = true)
    }
  }
}
