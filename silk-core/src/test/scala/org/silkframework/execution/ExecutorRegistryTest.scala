package org.silkframework.execution

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset.{Dataset, DatasetSpec, EmptyDataset}
import org.silkframework.execution.local.{LocalEntities, LocalExecution}
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor, Status}
import org.silkframework.runtime.plugin.PluginContext

class ExecutorRegistryTest extends AnyFlatSpec with Matchers {

  behavior of "ExecutorRegistry.executeWith"

  private implicit val pluginContext: PluginContext = PluginContext.empty
  private val task = PlainTask("task", DatasetSpec(EmptyDataset))
  private val execution = LocalExecution(useLocalInternalDatasets = true)

  it should "publish a successful status after a successful execution" in {
    val context = new ActivityMonitor[ExecutionReport]("test")
    ExecutorRegistry.executeWith(testExecutor((_, _) => None), task, Seq.empty, ExecutorOutput.empty, execution, context)
    context.status() should matchPattern { case Status.Finished(true, _, false, None) => }
  }

  it should "publish a failed status when the executor throws instead of staying in the running state" in {
    val context = new ActivityMonitor[ExecutionReport]("test")
    val failure = the[IllegalStateException] thrownBy {
      ExecutorRegistry.executeWith(testExecutor((_, _) => throw new IllegalStateException("executor failure")), task, Seq.empty, ExecutorOutput.empty, execution, context)
    }
    failure.getMessage shouldBe "executor failure"
    context.status() should matchPattern { case Status.Finished(false, _, false, Some(_)) => }
  }

  it should "publish a failed status when the execution produced a report with an error" in {
    val context = new ActivityMonitor[ExecutionReport]("test")
    val failingExecutor = testExecutor { (task, context) =>
      context.value.update(SimpleExecutionReport.initial(task).asFailed("report error"))
      None
    }
    ExecutorRegistry.executeWith(failingExecutor, task, Seq.empty, ExecutorOutput.empty, execution, context)
    context.status() should matchPattern { case Status.Finished(false, _, false, None) => }
  }

  private def testExecutor(body: (Task[DatasetSpec[Dataset]], ActivityContext[ExecutionReport]) => Option[LocalEntities]): Executor[DatasetSpec[Dataset], LocalExecution] = {
    new Executor[DatasetSpec[Dataset], LocalExecution] {
      override def execute(task: Task[DatasetSpec[Dataset]], inputs: Seq[LocalEntities], output: ExecutorOutput,
                           execution: LocalExecution, context: ActivityContext[ExecutionReport])
                          (implicit pluginContext: PluginContext): Option[LocalEntities] = {
        body(task, context)
      }
    }
  }
}
