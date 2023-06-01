package org.silkframework.workbench.workspace

import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.execution.Execution

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class WorkbenchAccessMonitorTest extends AnyFlatSpec with Matchers with TestUserContextTrait {
  behavior of "Workspace access monitor"

  it should "allow parallel access" in {
    implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(Execution.createFixedThreadPool(
      "project-import-thread",
      4
    ))
    val accessMonitor = new WorkbenchAccessMonitor()
    val resultFutures = for (i <- 1 to 100) yield {
      Future({
        accessMonitor.saveProjectAccess("project" + i)
        accessMonitor.getAccessItems.reverse
      })
    }
    Await.result(Future.sequence(resultFutures), 10.second)
  }
}
