package controllers.workflowApi.workflow

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config._
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.plugin.types.StringIterableParameter
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowOperator}
import org.silkframework.workspace.{ProjectConfig, WorkspaceFactory}

class DependentTasksInWorkflowRequestTest extends AnyFlatSpec with Matchers with TestUserContextTrait with BeforeAndAfterAll {

  behavior of "DependentTasksInWorkflowRequest"

  private val projectName = "DependentTasksInWorkflowRequestTest"

  override def beforeAll(): Unit = {
    super.beforeAll()
    PluginRegistry.registerPlugin(classOf[TestTask])
    WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))
  }

  override def afterAll(): Unit = {
    WorkspaceFactory().workspace.removeProject(projectName)
    super.afterAll()
  }

  it should "return all dependent tasks that are in a workflow" in {
    val project = WorkspaceFactory().workspace.project(projectName)

    // Add tasks without references
    project.addTask("1", TestTask())
    project.addTask("2", TestTask())
    project.addTask("3", TestTask())
    project.addTask("4", TestTask())

    // Add tasks that reference the first tasks
    project.addTask("A", TestTask(Seq("1")))
    project.addTask("B", TestTask(Seq("1", "2")))
    project.addTask("C", TestTask(Seq("3")))

    // Add a workflow that references some tasks
    project.addTask("Workflow", Workflow(Seq(operator("A"), operator("C"), operator("4"))))

    val result = DependentTasksInWorkflowRequest(projectName, "1", "Workflow")()
    result.tasks.map(_.taskId) should contain theSameElementsAs Seq("A")
  }

  private def operator(task: String): WorkflowOperator = {
    WorkflowOperator(inputs = Seq.empty, task = task, outputs = Seq.empty, Seq(), (0, 0), task, None, Seq.empty, Seq.empty)
  }

}

private case class TestTask(referenced: StringIterableParameter = Seq.empty) extends CustomTask {
  override def referencedTasks: Set[Identifier] = referenced.map(Identifier(_)).toSet
  override def inputPorts: InputPorts = FixedNumberOfInputs(Seq())
  override def outputPort: Option[Port] = None
}