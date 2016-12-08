package org.silkframework.execution

import java.lang.reflect.{ParameterizedType, Type, TypeVariable}
import java.util.logging.{Level, Logger}

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor}
import org.silkframework.runtime.plugin.{PluginDescription, PluginRegistry}
import org.silkframework.runtime.resource.EmptyResourceManager
import org.silkframework.runtime.validation.ValidationException

trait ExecutorRegistry {
  val log = Logger.getLogger("com.eccenca.di.workflow.ExecutorRegistry")

  /**
    * Retrieves an executor for a given task and execution type.
    */
  protected def executor[TaskType <: TaskSpec, ExecType <: ExecutionType](task: TaskType, context: ExecType): Executor[TaskType, ExecType] = {
    val plugins = PluginRegistry.availablePlugins[Executor[TaskType, ExecType]]
    val suitablePlugins = plugins.filter(isSuitable(task, context)).toList

    suitablePlugins match {
      case Nil =>
        throw new ValidationException(s"No executor found for task type ${task.getClass} and execution type ${context.getClass}. Available executors: ${plugins.mkString(", ")}.")
      case plugin :: Nil =>
        implicit val prefixes = Prefixes.empty
        implicit val resource = EmptyResourceManager
        plugin()
      case _ =>
        throw new ValidationException(s"Multiple executors found for task type ${task.getClass} and execution type ${context.getClass}")
    }
  }

  /**
    * Checks if an executor plugin is suitable for a given task and execution type.
    */
  private def isSuitable(task: TaskSpec, execution: ExecutionType)(plugin: PluginDescription[Executor[_, _]]): Boolean = {
    try {
      // Get executor interface
      val (executorInterface, inheritanceTrail) = findExecutorInterface(plugin.pluginClass).get
      // Get task and execution type
      val taskType = getTypeArgument(executorInterface, 0, inheritanceTrail)
      val executionType = getTypeArgument(executorInterface, 1, inheritanceTrail)
      // Check if suitable
      val suitableTaskType = taskType.isAssignableFrom(task.getClass)
      val suitableExecutionType = executionType.isAssignableFrom(execution.getClass)
      // Return true if both fits
      suitableTaskType && suitableExecutionType
    } catch {
      case e: MatchError =>
        log.log(Level.WARNING, "Problem checking executor interface for plugin " + plugin.id.toString + ": ", e)
        false
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
        superInterfaces.flatMap(c => findExecutorInterface(c, clazz :: inheritanceTrail)).headOption
    }
  }

  private def getTypeArgument(pt: ParameterizedType, index: Int, inheritanceTrail: List[Class[_]]): Class[_] = {
    pt.getActualTypeArguments.apply(index) match {
      case c: Class[_] => c
      case tv: TypeVariable[_] =>
        val actualType = for (descendent <- inheritanceTrail;
                              interface <- descendent.getGenericInterfaces if interface.isInstanceOf[ParameterizedType];
                              paramType = interface.asInstanceOf[ParameterizedType];
                              rawType = paramType.getRawType.asInstanceOf[Class[_]];
                              (typeParam, idx) <- rawType.getTypeParameters.zipWithIndex
                              if typeParam.getName == tv.getName && paramType.getActualTypeArguments()(idx).isInstanceOf[Class[_]]) yield {
          paramType.getActualTypeArguments()(idx).asInstanceOf[Class[_]]
        }
        actualType.headOption.getOrElse(throw new Exception("Type variable " + tv.getName + " could not be resolved!"))
    }
  }

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
