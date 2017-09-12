package org.silkframework.execution

import java.lang.reflect.{ParameterizedType, Type, TypeVariable}
import java.util.logging.{Level, Logger}
import java.lang.reflect.Modifier
import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor}
import org.silkframework.runtime.plugin.{PluginDescription, PluginRegistry}
import org.silkframework.runtime.resource.EmptyResourceManager
import org.silkframework.runtime.validation.ValidationException

trait ExecutorRegistry {

  private val log = Logger.getLogger(classOf[ExecutorRegistry].getName)

  /**
    * Retrieves an executor for a given task and execution type.
    */
  protected def executor[TaskType <: TaskSpec, ExecType <: ExecutionType](task: TaskType, context: ExecType): Executor[TaskType, ExecType] = {
    val plugins = PluginRegistry.availablePlugins[Executor[TaskType, ExecType]]
    val suitableExecutors = for(plugin <- plugins; taskType <- isSuitable(task, context, plugin)) yield (plugin, taskType)

    implicit val prefixes = Prefixes.empty
    implicit val resource = EmptyResourceManager

    suitableExecutors match {
      case Nil =>
        throw new ValidationException(s"No executor found for task type ${task.getClass} and execution type ${context.getClass}. Available executors: ${plugins.mkString(", ")}.")
      case (plugin, _) :: Nil =>
        plugin()
      case _ =>
        // Found multiple suitable executors => Choose the most specific one
        val sortedExecutors = suitableExecutors.sortWith((p1, p2) => p2._2.isAssignableFrom(p1._2))
        val mostSpecificPlugin = sortedExecutors.head._1
        mostSpecificPlugin()
    }
  }

  /**
    * Checks if an executor plugin is suitable for a given task and execution type.
    *
    * @return The actual supported task type, if the executor is suitable.
    */
  private def isSuitable(task: TaskSpec, execution: ExecutionType, plugin: PluginDescription[Executor[_, _]]): Option[Class[_]] = {
    try {
      // Get executor interface
      val (executorInterface, inheritanceTrail) = findExecutorInterface(plugin.pluginClass).get
      // Get task and execution type
      val taskType = getTypeArgument(executorInterface, 0, inheritanceTrail)
      val executionType = getTypeArgument(executorInterface, 1, inheritanceTrail)
      // Check if suitable
      val isAbstract = Modifier.isAbstract(plugin.pluginClass.getModifiers)
      val suitableTaskType = taskType.isAssignableFrom(task.getClass)
      val suitableExecutionType = executionType.isAssignableFrom(execution.getClass)
      // Return description if all fits
      if(!isAbstract && suitableTaskType && suitableExecutionType) {
        Some(taskType)
      } else {
        None
      }
    } catch {
      case e: MatchError =>
        log.log(Level.WARNING, "Problem checking executor interface for plugin " + plugin.id.toString + ": ", e)
        None
    }
  }

  private def findExecutorInterface(clazz: Class[_], inheritanceTrail: List[Class[_]] = List.empty): Option[(ParameterizedType, List[Class[_]])] = {
    val executorClasses: Set[Type] = Set(classOf[Executor[_, _]], classOf[DatasetExecutor[_, _]])
    clazz.getGenericInterfaces.collect { case pt: ParameterizedType => pt }.find(pt => executorClasses.contains(pt.getRawType)) match {
      case Some(executorInterface) =>
        Some((executorInterface, clazz :: inheritanceTrail))
      case None =>
        val superInterfaces =
          for (superInterface <- clazz.getGenericInterfaces) yield superInterface match {
            case c: Class[_] => c
            case pt: ParameterizedType => pt.getRawType.asInstanceOf[Class[_]]
          }
        val superTypes = superInterfaces ++ Option(clazz.getSuperclass)
        superTypes.flatMap(c => findExecutorInterface(c, clazz :: inheritanceTrail)).headOption
    }
  }

  private def getTypeArgument(pt: ParameterizedType, index: Int, inheritanceTrail: List[Class[_]]): Class[_] = {
    pt.getActualTypeArguments.apply(index) match {
      case c: Class[_] => c
      case tv: TypeVariable[_] =>
        val actualType = for (descendent <- inheritanceTrail;
                              interface <- descendent.getGenericInterfaces ++ Option(descendent.getGenericSuperclass) if interface.isInstanceOf[ParameterizedType];
                              paramType = interface.asInstanceOf[ParameterizedType];
                              rawType = paramType.getRawType.asInstanceOf[Class[_]];
                              (typeParam, idx) <- rawType.getTypeParameters.zipWithIndex
                              if typeParam.getName == tv.getName && paramType.getActualTypeArguments()(idx).isInstanceOf[Class[_]]) yield {
          paramType.getActualTypeArguments()(idx).asInstanceOf[Class[_]]
        }
        actualType.headOption.getOrElse(throw new Exception("Type variable " + tv.getName + " could not be resolved!"))
    }
  }

  private case class ExecutorDescription(plugin: PluginDescription[Executor[_, _]], taskType: Class[_], executorType: Class[_])

}

object ExecutorRegistry extends ExecutorRegistry {

  /** Fetch the most specific, matching Executor and execute it on the provided parameters. */
  def execute[TaskType <: TaskSpec, ExecType <: ExecutionType](task: Task[TaskType],
                                                               inputs: Seq[ExecType#DataType],
                                                               outputSchema: Option[EntitySchema],
                                                               execution: ExecType,
                                                               context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName)): Option[ExecType#DataType] = {

    val exec = executor(task.data, execution)
    exec.execute(task, inputs, outputSchema, execution, context)
  }
}
