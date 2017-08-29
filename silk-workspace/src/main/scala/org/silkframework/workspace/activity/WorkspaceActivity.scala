package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.Status
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}

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
  def taskOption: Option[ProjectTask[_]]

  /**
    * The status of this activity.
    */
  def status: Status

  /**
    * Holds the timestamp when the activity has been started.
    * Is None if the activity is not running at the moment.
    */
  def startTime: Option[Long] = None

}
