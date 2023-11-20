package org.silkframework.workspace.activity.workflow

import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.entity.Entity
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.plugin.{ParameterStringValue, ParameterValue, ParameterValues, PluginContext}

/**
 * Adds a method to tasks that reconfigures them based on entity values.
 */
object ReconfigureTask {

  implicit class ReconfigurableTask[T <: TaskSpec](task: Task[T]) {
    /**
     * Reconfigures a task based on entity values.
     *
     * @param task     The task to be reconfigured.
     * @param entities Task parameters will be updated based on the values in this entity.
     *                 Config parameters of later entities overwrite those of earlier inputs.
     * @return Task with updated parameters.
     */
    def reconfigure(entities: Seq[Entity])
                   (implicit pluginContext: PluginContext): Task[T] = {
      val parameters = task.data.parameters
      val configParameters = entities.
        map(entityToParameterValues(parameters, _)).
        foldLeft(ParameterValues.empty)(_ merge _)
      if (configParameters.values.isEmpty) {
        task
      } else {
        PlainTask(id = task.id, data = task.data.withParameters(configParameters), metaData = task.metaData).asInstanceOf[Task[T]]
      }
    }

    //TODO optimize
    private def entityToParameterValues(parameterValues: ParameterValues, entity: Entity, prefix: String = ""): ParameterValues = {
      val updatedValues: Iterable[(String, ParameterValue)] =
        for ((name, value) <- parameterValues.values.toSeq) yield {
          value match {
            case _: ParameterStringValue =>
              entity.schema.findPath(UntypedPath(prefix + name)) match {
                case Some(path) =>
                  entity.evaluate(path).headOption match {
                    case Some(entityValue) =>
                      name -> ParameterStringValue(entityValue)
                    case None =>
                      name -> value
                  }
                case None =>
                  name -> value
              }
            case values: ParameterValues =>
              name -> entityToParameterValues(values, entity, name + "-")
            case _ =>
              name -> value
          }
        }
      ParameterValues(updatedValues.toMap)
    }
  }
}


