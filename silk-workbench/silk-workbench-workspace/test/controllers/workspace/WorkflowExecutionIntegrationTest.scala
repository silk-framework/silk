package controllers.workspace

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{CustomTask, FixedNumberOfInputs, InputPorts, Port, Task}
import org.silkframework.execution.{ExecutionReport, ExecutionType, Executor, ExecutorOutput}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext, PluginRegistry}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow, WorkflowDatasetsParameter, WorkflowOperator, WorkflowOperatorsParameter}

class WorkflowExecutionIntegrationTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with Matchers {
    override def projectPathInClasspath: String = "diProjects/workflow-execution-integration-test-project.zip"

  override def workspaceProviderId: String = "inMemory"

  private val workflow = "workflow"
  private val dependencyWorkflow = "Dependencyexecutiontestworkflow_d61977481919ba4f"

  override def beforeAll(): Unit = {
    super.beforeAll()
    PluginRegistry.registerPlugin(classOf[CountingTask])
    PluginRegistry.registerPlugin(classOf[CountingTaskExecutor])
  }

  it should "execute a workflow that has flexible-noPort connections" in {
    val workflowExecutionActivity = project.task[Workflow](workflow).activity[LocalWorkflowExecutorGeneratingProvenance]
    workflowExecutionActivity.startBlocking(ParameterValues.empty)
  }

  it should "execute tasks with only a dependency output only once" in {
    val countingTaskId = "countingTask"
    project.addTask(countingTaskId, CountingTask())
    val workflow = project.task[Workflow](dependencyWorkflow)
    val updatedWorkflow = workflow.data.copy(
      operators = WorkflowOperatorsParameter(Seq(
        WorkflowOperator(Seq.empty, countingTaskId, Seq.empty, Seq.empty, (0, 0), countingTaskId, None, Seq.empty, Seq.empty)
      )),
      // Set counting task as dependency input and make sure that the dataset is executed first
      datasets = WorkflowDatasetsParameter(Seq(
        workflow.data.datasets.value.head.copy(dependencyInputs = Seq(countingTaskId), inputs = Seq.empty, outputPriority = Some(1.0))
      ))
    )
    val updatedWorkflowId = "updatedDependencyWorkflow"
    project.addTask(updatedWorkflowId, updatedWorkflow)
    CountingTask.counter = 0
    executeWorkflow(updatedWorkflowId)
    CountingTask.counter mustBe 1
  }
}

object CountingTask {
  @volatile
  var counter = 0
}
/** Task that counts its executions. */
case class CountingTask() extends CustomTask {
  override def inputPorts: InputPorts = InputPorts.NoInputPorts
  override def outputPort: Option[Port] = None
}

case class CountingTaskExecutor() extends Executor[CountingTask, ExecutionType] {
  override def execute(task: Task[CountingTask],
                       inputs: Seq[ExecutionType#DataType],
                       output: ExecutorOutput,
                       execution: ExecutionType,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[ExecutionType#DataType] = {
    CountingTask.counter += 1
    None
  }
}