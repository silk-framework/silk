package org.silkframework.workspace.resources

import org.silkframework.runtime.resource.ResourceNotFoundException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/**
  * Executes basic tests on a given resource repository.
  */
abstract class ResourceRepositoryTest extends AnyFlatSpec with Matchers  {

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
    testResource.loadAsString() mustBe testContent
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
    testResourceRead.loadAsString() mustBe testContent
    testResource.delete()
    testResource.exists mustBe false

    // Make sure that the base path is correct
    val basePath = if(project.basePath.isEmpty) "" else project.basePath + "/"
    normalize(project.child("child1").child("child2").basePath) mustBe normalize(s"${basePath}child1/child2")
    normalize(project.child("child1").child("child2").parent.get.basePath) mustBe normalize(s"${basePath}child1")
    normalize(project.child("non-existing").basePath) mustBe normalize(s"${basePath}non-existing")

    // Make sure that the resource path is correct
    normalize(project.child("child1").get("resource").path) mustBe normalize(s"${basePath}child1/resource")
    normalize(project.child("child1").child("child2").get("non-existing").path) mustBe normalize(s"${basePath}child1/child2/non-existing")
  }

  it should "not allow to open resources that do not exist" in {
    intercept[ResourceNotFoundException] {
      project.get("nonExistingResource", mustExist = true)
    }
  }

  // Normalizes a path so that the test works on Windows as well
  private def normalize(path: String): String = {
    path.replace('\\', '/')
  }
}
