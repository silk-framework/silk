package org.silkframework.workspace.activity

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity._
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.util.{Identifier, IdentifierGenerator}
import org.silkframework.workspace.{Project, ProjectTask}

import scala.collection.immutable.ListMap
import scala.reflect.ClassTag

/**
  * An activity that is either attached to a project (ProjectActivity) or a task (TaskActivity).
  */
abstract class WorkspaceActivity[ActivityType <: HasValue : ClassTag]() {

  private val identifierGenerator = new IdentifierGenerator(name)

  @volatile
  private var currentControl = createControl(Map.empty)

  @volatile
  private var controls: ListMap[Identifier, ActivityControl[ActivityType#ValueType]] = ListMap()

  /**
    * The project this activity belongs to.
    */
  def project: Project

  /**
    * The task this activity belongs to, if any.
    */
  def taskOption: Option[ProjectTask[_]]

  /**
    * The factory that is used to create new activity controls.
    */
  def factory: WorkspaceActivityFactory

  /**
    * Creates a new control for this activity type.
    */
  protected def createControl(config: Map[String, String]): ActivityControl[ActivityType#ValueType] // Activity(PluginDescription(defaultFactory.getClass)(config).apply(task))

  /**
    * The name of this activity.
    */
  final def name: Identifier = factory.pluginSpec.id

  final def config: Map[String, String] = PluginDescription(factory.getClass).parameterValues(factory)(Prefixes.empty)

  def allControls: ListMap[Identifier, ActivityControl[ActivityType#ValueType]] = {
    if(isSingleton) {
      ListMap((name, control))
    } else {
      controls
    }
  }

  /**
    * The most recent activity control that holds the status, value etc.
    */
  final def control: ActivityControl[ActivityType#ValueType] = currentControl

  /**
    * Convenience method to retrieve the current activity status.
    */
  final def status: Status = control.status()

  /**
    * Convenience method to retrieve the current activity value.
    */
  final def value: ActivityType#ValueType = control.value()

  /**
    * Holds the timestamp when the activity has been started.
    * Is None if the activity is not running at the moment.
    */
  final def startTime: Option[Long] = control.startTime

  /**
    * True, if there is always exactly one instance of this activity.
    */
  final def isSingleton: Boolean = factory.isSingleton

  /**
    * Starts an activity and returns immediately.
    * Optionally applies a supplied configuration to the started activity.
    *
    * @param config The activity parameters
    * @param user The user context
    * @return The identifier of the started activity
    */
  final def start(config: Map[String, String] = Map.empty)(implicit user: UserContext): Identifier = {
    val (id, control) = updateControl(config)
    control.start()
    id
  }

  /**
    * Starts an activity in blocking mode.
    * Optionally applies a supplied configuration to the started activity.
    *
    * @param config The activity parameters
    * @param user The user context
    * @return The identifier of the started activity
    */
  final def startBlocking(config: Map[String, String] = Map.empty)(implicit user: UserContext): Identifier = {
    val (id, control) = updateControl(config)
    control.startBlocking()
    id
  }

  @deprecated("should send configuration when calling start", "4.5.0")
  final def update(config: Map[String, String]): Unit = {
    updateControl(config)
  }

  protected final def updateControl(config: Map[String, String]): (Identifier, ActivityControl[ActivityType#ValueType]) = synchronized {
    val newControl = createControl(config)
    val identifier = if(isSingleton) name else identifierGenerator.generate("")

    if(isSingleton) {
      // Keep subscribers
      for (subscriber <- currentControl.status.subscribers) {
        newControl.status.subscribe(subscriber)
      }
      for (subscriber <- currentControl.value.subscribers) {
        newControl.value.subscribe(subscriber)
      }
    } else {
      if(controls.size >= WorkspaceActivity.MAX_CONTROLS_PER_ACTIVITY) {
        controls = controls.drop(1)
      }
      controls += ((identifier, newControl))
    }

    currentControl = newControl
    (identifier, newControl)
  }
}

object WorkspaceActivity {

  /**
    * The maximum number of controls that are held in memory for each activity type.
    * If more controls are created, the oldest ones are removed.
    */
  val MAX_CONTROLS_PER_ACTIVITY: Int = 10

}
