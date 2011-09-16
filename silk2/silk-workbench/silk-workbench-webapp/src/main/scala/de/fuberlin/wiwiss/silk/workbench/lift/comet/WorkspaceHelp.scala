package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.workspace.User
import xml.Text

class WorkspaceHelp extends Help {

  private val listener = User().onUpdate(_ => reRender())

  override def overview = {
    <div>
      Use the workspace to manage different projects.
      Each project consists of data sources, linking tasks and output tasks.
    </div>
  }

  override def actions = Text(
    if (User().workspace.projects.isEmpty) {
      "Create a new empty project or import an existing project."
    } else if (!User().projectOpen) {
      "Select the project you want to work on."
    } else if (User().project.sourceModule.tasks.isEmpty) {
      "Add the data sources you want to interlink."
    } else if (User().project.linkingModule.tasks.isEmpty) {
      "Add a linking task."
    } else {
      "Open a linking task for editing."
    }
  )
}