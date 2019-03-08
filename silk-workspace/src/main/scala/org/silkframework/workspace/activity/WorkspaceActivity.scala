package org.silkframework.workspace.activity

import java.util.logging.Logger

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

  /**
    * Generates new identifiers for created activity instances.
    */
  private val identifierGenerator = new IdentifierGenerator(name)
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  /**
    * Each workspace activity does have a current instance that's always defined.
    */
  @volatile
  private var currentInstance: ActivityControl[ActivityType#ValueType] = createInstance(Map.empty)

  /**
    * For non-singleton activities, this holds all instances.
    * If there are more instances than [[WorkspaceActivity.MAX_CONTROLS_PER_ACTIVITY]], the oldest ones are removed.
    */
  @volatile
  private var instances: ListMap[Identifier, ActivityControl[ActivityType#ValueType]] = ListMap()

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
  protected def createInstance(config: Map[String, String]): ActivityControl[ActivityType#ValueType]

  /**
    * Identifier of this activity.
    */
  final def name: Identifier = factory.pluginSpec.id

  /**
    * Human-readable label for this activity.
    */
  final def label: String = factory.pluginSpec.label

  /**
    * Retrieves all held instances of this activity type.
    * Instances are ordered from oldest to newest.
    */
  def allInstances: ListMap[Identifier, ActivityControl[ActivityType#ValueType]] = {
    if(isSingleton) {
      ListMap((name, control))
    } else {
      instances
    }
  }

  def instance(id: Identifier): ActivityControl[ActivityType#ValueType] = {
    allInstances.get(id) match {
      case Some(i) => i
      case None =>
        throw new NoSuchElementException(s"No activity instance with id $id.")
    }
  }

  /**
    * The most recent activity control that holds the status, value etc.
    */
  final def control: ActivityControl[ActivityType#ValueType] = currentInstance

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
    * @return The identifier of the started activity instance
    */
  final def start(config: Map[String, String] = Map.empty)(implicit user: UserContext): Identifier = {
    val (id, control) = addInstance(config)
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
    val (id, control) = addInstance(config)
    control.startBlocking()
    id
  }

  /**
    * The default configuration of this activity type.
    */
  final def defaultConfig: Map[String, String] = PluginDescription(factory.getClass).parameterValues(factory)(Prefixes.empty)

  @deprecated("should send configuration when calling start", "4.5.0")
  final def update(config: Map[String, String]): Unit = {
    addInstance(config)
  }

  /**
    * Adds a new instance of this activity type.
    * If this is a singleton activity, this will replace the previous instance.
    */
  protected final def addInstance(config: Map[String, String]): (Identifier, ActivityControl[ActivityType#ValueType]) = synchronized {
    val newControl = createInstance(config)
    val identifier = if(isSingleton) name else identifierGenerator.generate("")

    if(isSingleton) {
      // Keep subscribers
      for (subscriber <- currentInstance.status.subscribers) {
        newControl.status.subscribe(subscriber)
      }
      for (subscriber <- currentInstance.value.subscribers) {
        newControl.value.subscribe(subscriber)
      }
    } else {
      if(instances.size >= WorkspaceActivity.MAX_CONTROLS_PER_ACTIVITY) {
        log.warning(s"In project ${project.name} activity $name: Dropping an activity control instance because the control " +
            s"instance queue is full (max. ${WorkspaceActivity.MAX_CONTROLS_PER_ACTIVITY}. Dropped instance ID: ${instances.head._1}")
        instances = instances.drop(1)
      }
      instances += ((identifier, newControl))
    }

    currentInstance = newControl
    (identifier, newControl)
  }

  final def removeActivityInstance(instanceId: Identifier): Unit = synchronized {
    instances = instances - instanceId
  }
}

object WorkspaceActivity {

  /**
    * The maximum number of instances that are held in memory for each activity type.
    * If more instances are created, the oldest ones are removed.
    */
  val MAX_CONTROLS_PER_ACTIVITY: Int = 10

}
