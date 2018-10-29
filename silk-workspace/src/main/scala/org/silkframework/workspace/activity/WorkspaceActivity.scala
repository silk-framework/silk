package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.{ActivityControl, HasValue, Status, UserContext}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}
import scala.reflect.ClassTag

/**
  * An activity that is either attached to a project (ProjectActivity) or a task (TaskActivity).
  */
abstract class WorkspaceActivity[ActivityType <: HasValue : ClassTag] {

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
    * The most recent activity control that holds the status, value etc.
    */
  def control: ActivityControl[ActivityType#ValueType]

  /**
    * Convenience method to retrieve the current activity status.
    */
  def status: Status = control.status()

  /**
    * Convenience method to retrieve the current activity value.
    */
  def value: ActivityType#ValueType = control.value()

  /**
    * Holds the timestamp when the activity has been started.
    * Is None if the activity is not running at the moment.
    */
  def startTime: Option[Long] = control.startTime

  /**
    * True, if there is always exactly one instance of this activity.
    */
  def isSingleton: Boolean = false

  /**
    * Starts an activity and returns immediately.
    *
    * @param config The activity parameters
    * @param user The user context
    * @return The identifier of the started activity
    */
  def start(config: Map[String, String] = Map.empty)(implicit user: UserContext = UserContext.Empty): Identifier

  /**
    * Starts an activity in blocking mode.
    *
    * @param config The activity parameters
    * @param user The user context
    * @return The identifier of the started activity
    */
  def startBlocking(config: Map[String, String] = Map.empty)(implicit user: UserContext = UserContext.Empty): Identifier

}
