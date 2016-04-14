package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.Status
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, Task}

/**
  * An activity that is either attached to a project (ProjectActivity) or a task (TaskActivity).
  */
trait WorkspaceActivity {

  /**
    * The name of this activity.
    */
  def name: Identifier

  /**
    * The project this activity belongs to.
    */
  def project: Project

  /**
    * The task this activity belongs to if any.
    */
  def taskOption: Option[Task[_]]

  /**
    * The status of this activity.
    */
  def status: Status

}
