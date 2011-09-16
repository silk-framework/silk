package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.workspace.User

class LearnHelp extends Help {

  /**
   * Re-renders the widget if the current linking task has been changed.
   */
  private val taskListener = User().onUpdate {
    case User.CurrentTaskChanged(_) => reRender()
    case _ =>
  }

  override def overview = {
    <span>
      Learns linkage rules from reference links.
    </span>
  }

//  override def actions = {
//    if(User().linkingTask.referenceLinks.)
//  }
}