package org.silkframework.execution

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetAccess, DatasetSpec}
import org.silkframework.runtime.activity.Status.Running
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor, Status}
import org.silkframework.runtime.plugin.{PluginContext, PluginDescription, PluginRegistry}
import org.silkframework.runtime.validation.ValidationException

import java.lang.reflect.{Modifier, ParameterizedType, TypeVariable}
import java.util.logging.{Level, Logger}
import scala.language.existentials

trait ExecutorRegistry {

  private val log = Logger.getLogger(classOf[ExecutorRegistry].getName)

  /**
    * Retrieves an executor for a given task and execution type.
    */
  protected def executor[TaskType <: TaskSpec, ExecType <: ExecutionType](task: TaskType, context: ExecType): Executor[TaskType, ExecType] = {
    val plugins = PluginRegistry.availablePlugins[Executor[TaskType, ExecType]]

    // If the task to be executed is a dataset, we need to forward the Dataset plugin.
    val taskClass = task match {
      case datasetSpec: DatasetSpec[_] =>
        datasetSpec.plugin.getClass
      case _ =>
        task.getClass
    }

    val suitableExecutors = for(plugin <- plugins; taskType <- isSuitable(taskClass, context, plugin)) yield (plugin, taskType)

    implicit val pluginContext: PluginContext = PluginContext.empty

    suitableExecutors.size match {
      case 0 =>
        throw new ValidationException(s"No executor found for task type ${taskClass.getSimpleName} " +
            s"and execution type ${context.getClass.getSimpleName}. Available executors: ${plugins.mkString(", ")}.")
      case 1 =>
        // Instantiate executor
        suitableExecutors.head._1.apply()(PluginContext.empty)
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
  private def isSuitable(taskClass: Class[_], execution: ExecutionType, plugin: PluginDescription[Executor[_, _]]): Option[Class[_]] = {
    try {
      // Get executor interface
      val (executorInterface, inheritanceTrail) = findExecutorInterface(plugin.pluginClass).get
      // Get task and execution type
      val taskType = getTypeArgument(executorInterface, 0, inheritanceTrail)
      val executionType = getTypeArgument(executorInterface, 1, inheritanceTrail)
      // Check if suitable
      val isAbstract = Modifier.isAbstract(plugin.pluginClass.getModifiers)
      val suitableTaskType = taskType.isAssignableFrom(taskClass)
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
    val executorClass =
      if(classOf[DatasetExecutor[_, _]].isAssignableFrom(clazz)) {
        classOf[DatasetExecutor[_, _]]
      } else {
        classOf[Executor[_, _]]
      }

    clazz.getGenericInterfaces.collect { case pt: ParameterizedType => pt }.find(_.getRawType == executorClass) match {
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
                              interface <- descendent.getGenericInterfaces.toSeq ++ Option(descendent.getGenericSuperclass) if interface.isInstanceOf[ParameterizedType];
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

  private lazy val executionManager = {
    implicit val pluginContext: PluginContext = PluginContext.empty
    PluginRegistry.createFromConfig[ExecutionManager]("execution.manager")
  }

  def execution(): ExecutionType = {
    executionManager.current()
  }

  /** Fetch the most specific, matching Executor and execute it on the provided parameters. */
  def execute[TaskType <: TaskSpec, ExecType <: ExecutionType](
    task: Task[TaskType],
    inputs: Seq[ExecType#DataType],
    output: ExecutorOutput,
    execution: ExecType,
    context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName)
  )(implicit pluginContext: PluginContext): Option[ExecType#DataType] = {
    val exec = executor(task.data, execution)
    context.status.update(Status.Running("Running", None), logStatus = false)
    val startTime = System.currentTimeMillis()
    val result = exec.execute(task, inputs, output, execution, context)
    context.status.update(Status.Finished(success = true, System.currentTimeMillis() - startTime, cancelled = false), logStatus = false)
    result
  }

  /** Fetch the execution specific access to a dataset.*/
  def access[DatasetType <: Dataset, ExecType <: ExecutionType](task: Task[DatasetSpec[DatasetType]],
                                                                execution: ExecType): DatasetAccess = {
    executor(task.data, execution) match {
      case ds: DatasetExecutor[DatasetType, ExecType] =>
        ds.access(task, execution)
      case _ =>
        throw new Exception(s"Tried to access task $task, which does not provide a dataset executor.")
    }
  }
}
