package org.silkframework.workspace


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{CustomTask, InputPorts, MetaData, Port}
import org.silkframework.rule.{DatasetSelection, TransformSpec}
import org.silkframework.runtime.activity.{SimpleUserContext, TestUserContextTrait}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.users.DefaultUserManager
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.WorkspaceTest.RecordingWorkspaceProvider
import org.silkframework.workspace.exceptions.CircularDependencyException

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

  it should "use the loading user for provider write calls when access control is enabled" in {
    ConfigTestTrait.withConfig("workspace.accessControl.enabled" -> Some("true")) {
      val recordingProvider = new RecordingWorkspaceProvider()
      val adminUser = SimpleUserContext(Some(DefaultUserManager.get("urn:admin")))
      val regularUser = SimpleUserContext(Some(DefaultUserManager.get("urn:regular")))

      // Register project in the provider so the Project constructor can load it
      val projectId = "aclTestProject"
      recordingProvider.putProject(ProjectConfig(projectId, metaData = MetaData(Some(projectId))))(adminUser)

      // Create a Project with adminUser as loadingUser
      val project = new Project(ProjectConfig(projectId, metaData = MetaData(Some(projectId))), recordingProvider, new InMemoryResourceManager, adminUser)
      recordingProvider.recordedUsers.clear()

      // Add task with regular user — provider should receive admin user
      project.addAnyTask("task1", ProjectTestTask())(regularUser)
      recordingProvider.recordedUsers should contain(("putTask", adminUser))

      recordingProvider.recordedUsers.clear()

      // Update task with regular user — provider should receive admin user
      project.updateAnyTask("task1", ProjectTestTask("updated"))(regularUser)
      recordingProvider.recordedUsers should contain(("putTask", adminUser))

      recordingProvider.recordedUsers.clear()

      // Remove task with regular user — provider should receive admin user
      project.removeAnyTask("task1", removeDependentTasks = false)(regularUser)
      recordingProvider.recordedUsers should contain(("deleteTask", adminUser))
    }
  }

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

}

case class ProjectTestTask(testParam: String = "test value") extends CustomTask {
  override def inputPorts: InputPorts = InputPorts.NoInputPorts
  override def outputPort: Option[Port] = None
}
