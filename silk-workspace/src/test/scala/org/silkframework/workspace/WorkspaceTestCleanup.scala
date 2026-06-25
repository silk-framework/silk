package org.silkframework.workspace

import org.silkframework.runtime.activity.{ActivityControl, ActivityExecution, UserContext}

import java.util.concurrent.TimeUnit
import scala.util.Try

private[workspace] object WorkspaceTestCleanup {

  def stop(workspace: Workspace)(implicit userContext: UserContext): Unit = {
    val projects = Try(workspace.allProjects).getOrElse(Seq.empty)
    val projectControls = projects.flatMap(project => project.activities.map(_.control) ++ project.allTasks.flatMap(_.activities.map(_.control)))
    val workspaceControls = workspace.activities.map(_.control)

    (projectControls ++ workspaceControls).foreach(control => Try(control.cancel()))
    (projectControls ++ workspaceControls).foreach(waitForActivityStop)

    // Give the shared activity pools a chance to finish queued cancellation and restart bookkeeping before the next
    // suite or test creates another workspace.
    ActivityExecution.forkJoinPool.awaitQuiescence(5, TimeUnit.SECONDS)
    ActivityExecution.priorityThreadPool.awaitQuiescence(5, TimeUnit.SECONDS)
  }

  private def waitForActivityStop(control: ActivityControl[_]): Unit = {
    Try(control.waitUntilFinished())
  }
}
