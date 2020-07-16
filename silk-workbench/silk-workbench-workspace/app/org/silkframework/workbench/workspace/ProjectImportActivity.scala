package org.silkframework.workbench.workspace

import java.io.File
import java.util.UUID

import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.workspace.{ProjectMarshallingTrait, WorkspaceFactory}

class ProjectImportActivity(projectLabel: String, file: File, marshaller: ProjectMarshallingTrait) extends Activity[Unit] {

  override def run(context: ActivityContext[Unit])(implicit userContext: UserContext): Unit = {
    context.status.updateMessage("Importing project", logStatus = false)

    // TODO generate better id
    val projectId = UUID.randomUUID().toString
    // TODO add label
    WorkspaceFactory().workspace.importProject(projectId, file, marshaller)
  }
}
