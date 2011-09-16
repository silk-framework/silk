package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.workspace.User
import xml.{NodeSeq, Text}

class ReferenceLinksHelp extends LinksHelp {

  private val taskListener = User().onUpdate {
    case User.CurrentTaskChanged(_) => reRender()
    case _ =>
  }

  override def renderOverview = {
    <div>
      The reference links of this linking task.
    </div>
  }

  override def renderActions = {
    val alignment = User().linkingTask.alignment
    if(alignment.isEmpty) {
      Text("This linking task does not contain any reference links yet.") ++
      howToAddReferenceLinks
    } else if(alignment.positive.isEmpty) {
      Text("This linking task does not contain any positive reference links yet.") ++
      howToAddReferenceLinks
    } else if(alignment.negative.isEmpty) {
      Text("This linking task does not contain any negative reference links yet.") ++
      howToAddReferenceLinks
    } else {
      NodeSeq.Empty
    }
  }
}