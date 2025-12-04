package org.silkframework.workspace


import org.silkframework.rule.{DatasetSelection, TransformSpec}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.workspace.exceptions.CircularDependencyException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProjectTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait {

  behavior of "Project"

  it should "not allow circular dependencies between tasks" in {
    val project = retrieveOrCreateProject("CircularDependencyTest")

    // Three tasks that create a forbidden circular dependency
    val transform1 = TransformSpec(selection = DatasetSelection("transform2"))
    val transform2 = TransformSpec(selection = DatasetSelection("transform3"))
    val transform3 = TransformSpec(selection = DatasetSelection("transform1"))

    // Adding the first two tasks is fine
    project.addTask("transform1", transform1)
    project.addTask("transform2", transform2)

    // Try to add a task that would create a circular dependency
    val thrown1 = the[CircularDependencyException] thrownBy project.addTask("transform3", transform3)
    thrown1.circularTaskChain shouldBe Seq("transform3", "transform1", "transform2", "transform3")

    val thrown2 = the[CircularDependencyException] thrownBy project.addAnyTask("transform3", transform3)
    thrown2.circularTaskChain shouldBe Seq("transform3", "transform1", "transform2", "transform3")

    // Try to update a task that would create a circular dependency
    val thrown3 = the[CircularDependencyException] thrownBy project.updateTask("transform2", transform3)
    thrown3.circularTaskChain shouldBe Seq("transform2", "transform1", "transform2")

    val thrown4 = the[CircularDependencyException] thrownBy project.updateAnyTask("transform2", transform3)
    thrown4.circularTaskChain shouldBe Seq("transform2", "transform1", "transform2")
  }

  it should "not allow that a task references itself" in {
    val project = retrieveOrCreateProject("SelfReferenceTest")
    val transform1 = TransformSpec(selection = DatasetSelection("transform1"))
    val thrown1 = the[CircularDependencyException] thrownBy project.addTask("transform1", transform1)
    thrown1.circularTaskChain shouldBe Seq("transform1", "transform1")
  }

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

}
