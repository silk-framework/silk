package org.silkframework.workspace.activity

import java.lang.reflect.{ParameterizedType, Type, TypeVariable}

import org.silkframework.config.{Prefixes, TaskSpec}
import org.silkframework.runtime.activity.{Activity, ActivityControl, HasValue, UserContext}
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.{Identifier, IdentifierGenerator}
import org.silkframework.workspace.{Project, ProjectTask}

import scala.reflect.ClassTag
import scala.runtime.BoxedUnit

/**
  * Holds an activity that is part of an task.
  *
  * @param task           The task this activity belongs to.
  * @param defaultFactory The initial activity factory for generating the activity.
  * @tparam DataType The type of the task.
  */
class TaskActivity[DataType <: TaskSpec : ClassTag, ActivityType <: HasValue : ClassTag](val task: ProjectTask[DataType],
                                                                                         defaultFactory: TaskActivityFactory[DataType, ActivityType])
    extends WorkspaceActivity[ActivityType] {

  @volatile
  private var currentControl = Activity{
    defaultFactory(task)
  }

  @volatile
  private var controls: Map[Identifier, ActivityControl[ActivityType#ValueType]] = Map()

  private val identifierGenerator = new IdentifierGenerator(defaultFactory.pluginSpec.id)

  override def name: Identifier = defaultFactory.pluginSpec.id

  override def project: Project = task.project

  override def taskOption: Option[ProjectTask[DataType]] = Option(task)

  override def control: ActivityControl[ActivityType#ValueType] = currentControl

  def allControls: Map[Identifier, ActivityControl[ActivityType#ValueType]] = {
    if(defaultFactory.isSingleton) {
      Map((name, control))
    } else {
      controls
    }
  }

  def factory: TaskActivityFactory[DataType, ActivityType] = defaultFactory

  def autoRun: Boolean = defaultFactory.autoRun

  def isSingleton: Boolean = defaultFactory.isSingleton

  def config: Map[String, String] = PluginDescription(defaultFactory.getClass).parameterValues(defaultFactory)(Prefixes.empty)

  def reset()(implicit userContext: UserContext): Unit = {
    currentControl.cancel()
    createControl(config)
  }

  /**
    * Starts the activity asynchronously.
    * Optionally applies a supplied configuration.
    */
  def start(config: Map[String, String] = Map.empty)(implicit user: UserContext): Identifier = {
    val (id, control) = createControl(config)
    control.start()
    id
  }

  /**
    * Starts the activity blocking.
    * Optionally applies a supplied configuration.
    */
  def startBlocking(config: Map[String, String] = Map.empty)(implicit user: UserContext): Identifier = {
    val (id, control) = createControl(config)
    control.startBlocking()
    id
  }

  @deprecated("should send configuration when calling start", "4.5.0")
  def update(config: Map[String, String]): Unit = {
    createControl(config)
  }

  private def createControl(config: Map[String, String]): (Identifier, ActivityControl[ActivityType#ValueType]) = {
    implicit val prefixes: Prefixes = task.project.config.prefixes
    implicit val resources: ResourceManager = task.project.resources
    val newControl = Activity(PluginDescription(defaultFactory.getClass)(config).apply(task))
    val identifier = if(defaultFactory.isSingleton) name else identifierGenerator.generate("")

    if(defaultFactory.isSingleton) {
      // Keep subscribers
      for (subscriber <- currentControl.status.subscribers) {
        newControl.status.subscribe(subscriber)
      }
      for (subscriber <- currentControl.value.subscribers) {
        newControl.value.subscribe(subscriber)
      }
    } else {
      controls += ((identifier, newControl))
    }

    currentControl = newControl
    (identifier, newControl)
  }

  def activityType: Class[_] = defaultFactory.activityType

  /**
    * Checks if the value type of the activity is Unit.
    */
  def isUnitValueType: Boolean = {
    val activityClassName = classOf[Activity[_]].getName
    val activityInterface = {
      val at = activityType
      val gi = getAllInterfacesRecursively(at, activityClassName)
      gi.find(_.getTypeName.startsWith(activityClassName)) match {
        case Some(activityTrait) =>
          activityTrait.asInstanceOf[ParameterizedType]
        case None =>
          throw new Exception("Not able to get value type of activity " + at.getName)
      }
    }
    val valueType = activityInterface.getActualTypeArguments.apply(0)
    val valueClass = valueType match {
      case pt: ParameterizedType => pt.getRawType.asInstanceOf[Class[_]]
      // FIXME: This is not correct in general. For type variables we would have to dig deeper what the actual type is, but this works correctly for all cases currently, since CachedActivities always have a value.
      case tv: TypeVariable[_] => tv.getGenericDeclaration.asInstanceOf[Class[_]]
      case t: Type => t.asInstanceOf[Class[_]]
    }
    valueClass == classOf[BoxedUnit]
  }

  private def getAllInterfacesRecursively(clazz: Type, stopAtClassPrefix: String): List[Type] = {
    if(clazz.getTypeName.startsWith(stopAtClassPrefix)) {
      List(clazz)
    } else {
      val recursiveTypes: List[Type] = clazz match {
        case c: Class[_] =>
          val genericInterfaces = c.getGenericInterfaces.toList
          val transitiveInterfaces = (genericInterfaces ++ Option(c.getSuperclass).toSeq).flatMap(getAllInterfacesRecursively(_, stopAtClassPrefix))
          genericInterfaces ++ transitiveInterfaces
        case pt: ParameterizedType =>
          getAllInterfacesRecursively(pt.getRawType, stopAtClassPrefix)
        case t: Type =>
          List()
      }
      clazz :: recursiveTypes
    }
  }
}
