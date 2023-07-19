package org.silkframework.workspace.activity

import org.silkframework.config.{DefaultConfig, TaskSpec}
import org.silkframework.runtime.activity._
import org.silkframework.runtime.plugin.{ClassPluginDescription, ParameterValues, PluginContext}
import org.silkframework.runtime.validation.ServiceUnavailableException
import org.silkframework.util.{Identifier, IdentifierGenerator}
import org.silkframework.workspace.{Project, ProjectTask}

import java.time.Instant
import java.util.logging.Logger
import scala.collection.immutable.ListMap
import scala.reflect.ClassTag
import scala.util.Try

/**
  * An activity that is either attached to a project (ProjectActivity) or a task (TaskActivity) or is a GlobalWorkspaceActivity.
  */
abstract class WorkspaceActivity[ActivityType <: HasValue : ClassTag]() {

  /**
    * Generates new identifiers for created activity instances.
    */
  private val identifierGenerator = new IdentifierGenerator(name)
  private val log: Logger = Logger.getLogger(this.getClass.getName)
  lazy private val maxConcurrentExecutionsPerActivity = WorkspaceActivity.maxConcurrentExecutionsPerActivity()

  /**
    * Each workspace activity does have a current instance that's always defined.
    */
  @volatile
  private var currentInstance: ActivityControl[ActivityType#ValueType] = createInstanceFromParameterValues(ParameterValues.empty)

  /**
    * The plugin parameters of current instance.
    */
  @volatile
  private var currentParameters: ParameterValues = ParameterValues.empty

  /**
    * For non-singleton activities, this holds all instances.
    * If there are more instances than [[maxConcurrentExecutionsPerActivity]], the oldest finished ones are removed.
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
  def taskOption: Option[ProjectTask[_ <: TaskSpec]]

  /**
    * The factory that is used to create new activity controls.
    */
  def factory: WorkspaceActivityFactory

  @deprecated("Use createInstanceFromParameterValues instead.")
  protected def createInstance(config: Map[String, String]): ActivityControl[ActivityType#ValueType] = {
    createInstanceFromParameterValues(ParameterValues.fromStringMap(config))
  }

  /**
    * Creates a new control for this activity type.
    */
  protected def createInstanceFromParameterValues(config: ParameterValues): ActivityControl[ActivityType#ValueType]

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
    * Updates the value of this activity.
    *
    * @throws UnsupportedOperationException If the value can not be updated.
    */
  def updateValue(value: ActivityType#ValueType): Unit = {
    control.value match {
      case holder: ValueHolder[ActivityType#ValueType] =>
        holder.update(value)
      case _ =>
        throw new UnsupportedOperationException("Cannot update value of this activity.")
    }
  }

  /**
    * Holds the timestamp when the activity has been started.
    * Is None if the activity is not running at the moment.
    */
  final def startTime: Option[Instant] = control.startTime

  /**
    * The user that started the activity. Refers to the empty user until the activity has been started the first time.
    */
  final def startedBy: UserContext = control.startedBy

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
  final def start(config: ParameterValues)(implicit user: UserContext): Identifier = {
    val (id, control) = addInstance(config)
    control.start()
    id
  }

  @deprecated("Use the variant with ParameterValues instead.")
  final def start(config: Map[String, String] = Map.empty)(implicit user: UserContext): Identifier = {
    start(ParameterValues.fromStringMap(config))
  }

  /**
    * Starts an activity in blocking mode.
    * Optionally applies a supplied configuration to the started activity.
    *
    * @param config The activity parameters
    * @param user The user context
    * @return The identifier of the started activity
    */
  final def startBlocking(config: ParameterValues)(implicit user: UserContext): Identifier = {
    val (id, control) = addInstance(config)
    control.startBlocking()
    id
  }

  @deprecated("Use the variant with ParameterValues instead.")
  final def startBlocking(config: Map[String, String] = Map.empty)(implicit user: UserContext): Identifier = {
    startBlocking(ParameterValues.fromStringMap(config))
  }

  /**
    * Starts an activity in blocking mode and returns the result of the activity execution.
    * The activity instance is removed automatically after the activity has finished.
    * Optionally applies a supplied configuration to the started activity.
    *
    * @param config The activity parameters
    * @param user The user context
    * @return The identifier of the started activity
    */
  final def startBlockingAndGetValue(config: ParameterValues)(implicit user: UserContext): ActivityType#ValueType = {
    val (id, control) = addInstance(config)
    control.startBlocking()
    val value = control.value()
    removeActivityInstance(id)
    value
  }

  @deprecated("Use the variant with ParameterValues instead.")
  final def startBlockingAndGetValue(config: Map[String, String] = Map.empty)(implicit user: UserContext): ActivityType#ValueType = {
    startBlockingAndGetValue(ParameterValues.fromStringMap(config))
  }

  /**
    * Starts the current activity instance prioritized.
    */
  final def startPrioritized()(implicit user: UserContext): Unit = {
    currentInstance.startPrioritized()
  }

  /**
    * The default configuration of this activity type.
    */
  final def defaultConfig: Map[String, String] = {
    implicit val pluginContext: PluginContext = PluginContext.empty
    ClassPluginDescription(factory.getClass).parameterValues(factory).toStringMap
  }

  @deprecated("should send configuration when calling start", "4.5.0")
  final def update(config: Map[String, String]): Unit = {
    addInstance(ParameterValues.fromStringMap(config))
  }

  /** Marks an activity as a cache activity, i.e. an activity that stores a cached value of something that is potentially expensive to compute. */
  def isCacheActivity: Boolean = factory.isCacheActivity

  /** True if this activity caches data that may be derived from a dataset. */
  def isDatasetRelatedCache: Boolean = factory.isDatasetRelatedCache

  /** Only if this is a cached activity start the activity dirty, i.e. even if the activity is currently running, it will run again. */
  def startDirty(reloadCacheFile: Boolean = false)
                (implicit user: UserContext): Unit = {
    if(isCacheActivity) {
      val a = control.underlying
      a match {
        case cacheActivity: CachedActivity[_] =>
          cacheActivity.startDirty(control, reloadCacheFile)
        case _ =>
      }
    }
  }

  /**
    * The class of the activity value.
    */
  def activityType: Class[_] = factory.activityType

  /**
    * Adds a new instance of this activity type.
    * If this is a singleton activity, it will only be updated if the configuration changed.
    */
  protected final def addInstance(config: ParameterValues): (Identifier, ActivityControl[ActivityType#ValueType]) = synchronized {
    val identifier = if(isSingleton) name else identifierGenerator.generate("")

    if(isSingleton) {
      if(config != currentParameters) {
        currentInstance = createControl(config)
      }
    } else {
      val newControl = createControl(config)
      if(instances.size >= maxConcurrentExecutionsPerActivity) {
        val configInfo = s"This limit can be increased via config key '${WorkspaceActivity.MAX_CONCURRENT_EXECUTIONS_CONFIG_KEY}'."
        val activityDescription = projectOpt.map(p => s"In project ${p.id} activity '$name'").getOrElse(s"In workspace activity '$name'")
        instances.find(instance => instance._2.status.get.exists(_.finished())) match {
          case Some(instance) =>
            log.warning(s"$activityDescription: Dropping an activity control instance because the control " +
              s"instance queue is full (max. $maxConcurrentExecutionsPerActivity. Dropped instance ID: ${instance._1}." +
              s" Make sure to remove consumed results by the client. $configInfo")
            removeActivityInstance(instance._1)
          case None =>
            // Cannot remove running instances, abort.
            throw ServiceUnavailableException(s"$activityDescription: Cannot start another instance of activity '$label' because " +
              s"limit ($maxConcurrentExecutionsPerActivity) of concurrently running instances has been reached. $configInfo")
        }
      }
      instances += ((identifier, newControl))
      currentInstance = newControl
    }

    currentParameters = config
    (identifier, currentInstance)
  }

  private def createControl(config: ParameterValues): ActivityControl[ActivityType#ValueType] = {
    val newControl = createInstanceFromParameterValues(config)
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

  val MAX_CONCURRENT_EXECUTIONS_CONFIG_KEY = "org.silkframework.runtime.activity.concurrentExecutions"

  /**
    * The maximum number of instances that are held in memory for each activity type.
    * If more instances are created, the oldest ones are removed.
    */
  def maxConcurrentExecutionsPerActivity(): Int = {
    val cfg = DefaultConfig.instance()
    val defaultValue = 20
    if(cfg.hasPath(MAX_CONCURRENT_EXECUTIONS_CONFIG_KEY)) {
      Try(cfg.getInt(MAX_CONCURRENT_EXECUTIONS_CONFIG_KEY)).getOrElse(defaultValue)
    } else {
      defaultValue
    }
  }
}
