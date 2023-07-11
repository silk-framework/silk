package org.silkframework.workspace.activity

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity._
import org.silkframework.runtime.plugin.{ClassPluginDescription, ParameterValues, PluginContext}
import org.silkframework.workspace.{Project, ProjectTask}

import java.lang.reflect.{ParameterizedType, Type, TypeVariable}
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

  def project: Project = task.project

  override def projectOpt: Option[Project] = Some(project)

  override def taskOption: Option[ProjectTask[DataType]] = Option(task)

  override def factory: TaskActivityFactory[DataType, ActivityType] = defaultFactory

  def autoRun: Boolean = defaultFactory.autoRun

  override protected def createInstanceFromParameterValues(config: ParameterValues): ActivityControl[ActivityType#ValueType] = {
    implicit val pluginContext: PluginContext = PluginContext(project.config.prefixes, project.resources)
    Activity(
      ClassPluginDescription(defaultFactory.getClass)(config, ignoreNonExistingParameters = false).apply(task),
      projectAndTaskId = Some(ProjectAndTaskIds(project.id, taskOption.map(_.id)))
    )
  }

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

