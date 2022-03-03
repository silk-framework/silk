package org.silkframework.workspace.activity

import java.time.Instant
import java.util.logging.Logger

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.{ObservableMirror, _}
import org.silkframework.runtime.plugin.ClassPluginDescription
import org.silkframework.util.{Identifier, IdentifierGenerator}
import org.silkframework.workspace.{Project, ProjectTask}

import scala.collection.immutable.ListMap
import scala.reflect.ClassTag

/**
  * An activity that is either attached to a project (ProjectActivity) or a task (TaskActivity) or is a GlobalWorkspaceActivity.
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
    * The plugin parameters of current instance.
    */
  @volatile
  private var currentParameters: Map[String, String] = Map.empty

  /**
    * For non-singleton activities, this holds all instances.
    * If there are more instances than [[WorkspaceActivity.MAX_CONTROLS_PER_ACTIVITY]], the oldest ones are removed.
    */
  @volatile
  private var instances: ListMap[Identifier, ActivityControl[ActivityType#ValueType]] = ListMap()

  /**
    * The project this activity belongs to, if any.
    */
  def projectOpt: Option[Project]

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
    * Human-readable description for this activity.
    */
  final def description: String = factory.pluginSpec.description

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
    * Retrieves the activity status of the current control.
    */
  final val status: Observable[Status] = new ObservableMirror(control.status)

  /**
    * Retrieves the activity value of the current control.
    */
  final val value: Observable[ActivityType#ValueType] = new ObservableMirror(control.value)

  /**
    * Holds the timestamp when the activity has been started.
    * Is None if the activity is not running at the moment.
    */
  final def startTime: Option[Instant] = control.startTime

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
  final def defaultConfig: Map[String, String] = ClassPluginDescription(factory.getClass).parameterValues(factory)(Prefixes.empty)

  @deprecated("should send configuration when calling start", "4.5.0")
  final def update(config: Map[String, String]): Unit = {
    addInstance(config)
  }

  /** Marks an activity as a cache activity, i.e. an activity that stores a cached value of something that is potentially expensive to compute. */
  def isCacheActivity: Boolean = factory.isCacheActivity

  /**
    * Adds a new instance of this activity type.
    * If this is a singleton activity, it will only be updated if the configuration changed.
    */
  protected final def addInstance(config: Map[String, String]): (Identifier, ActivityControl[ActivityType#ValueType]) = synchronized {
    val identifier = if(isSingleton) name else identifierGenerator.generate("")

    if(isSingleton) {
      if(config != currentParameters) {
        currentInstance = createControl(config)
      }
    } else {
      val newControl = createControl(config)
      if(instances.size >= WorkspaceActivity.MAX_CONTROLS_PER_ACTIVITY) {
        val activityDescription = projectOpt.map(p => s"In project ${p.name} activity '$name'").getOrElse(s"In workspace activity '$name'")
        log.warning(s"$activityDescription: Dropping an activity control instance because the control " +
            s"instance queue is full (max. ${WorkspaceActivity.MAX_CONTROLS_PER_ACTIVITY}. Dropped instance ID: ${instances.head._1}")
        instances = instances.drop(1)
      }
      instances += ((identifier, newControl))
      currentInstance = newControl
    }

    currentParameters = config
    (identifier, currentInstance)
  }

  private def createControl(config: Map[String, String]): ActivityControl[ActivityType#ValueType] = {
    val newControl = createInstance(config)
    // Update the status and value mirrors to point to the new instance
    status.asInstanceOf[ObservableMirror[Status]].updateObservable(newControl.status)
    value.asInstanceOf[ObservableMirror[ActivityType#ValueType]].updateObservable(newControl.value)
    newControl
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
  val MAX_CONTROLS_PER_ACTIVITY: Int = 20

}
