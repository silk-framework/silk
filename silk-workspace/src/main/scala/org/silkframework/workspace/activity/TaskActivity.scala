package org.silkframework.workspace.activity

import java.lang.reflect.{ParameterizedType, Type, TypeVariable}

import org.silkframework.config.{Prefixes, TaskSpec}
import org.silkframework.runtime.activity.{Activity, ActivityControl, HasValue, UserContext}
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.runtime.resource.ResourceManager
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

  override def project: Project = task.project

  override def taskOption: Option[ProjectTask[DataType]] = Option(task)

  override def factory: TaskActivityFactory[DataType, ActivityType] = defaultFactory

  def autoRun: Boolean = defaultFactory.autoRun

  protected override def createInstance(config: Map[String, String]): ActivityControl[ActivityType#ValueType] = {
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val resources: ResourceManager = project.resources
    Activity(PluginDescription(defaultFactory.getClass)(config).apply(task))
  }

  def reset()(implicit userContext: UserContext): Unit = {
    control.cancel()
    addInstance(defaultConfig)
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

