package org.silkframework.workspace.activity.workflow

import org.silkframework.config.{FixedSchemaPort, PlainTask, Port, Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin._
import org.silkframework.workspace.ProjectTrait

import scala.collection.mutable

/**
 * Groups methods related to the reconfiguration of tasks.
 */
object ReconfigureTasks {

  implicit class ReconfigurablePluginDescription(pluginDesc: PluginDescription[_]) {
    /**
     * Returns the list of properties that are available to reconfigure this plugin.
     */
    def configProperties: IndexedSeq[String] = {
      val buffer = mutable.Buffer[String]()
      collectConfigProperties(buffer)
      buffer.toIndexedSeq
    }

    private def collectConfigProperties(buffer: mutable.Buffer[String], prefix: String = ""): Unit = {
      for (param <- pluginDesc.parameters) {
        param.parameterType match {
          case _: StringParameterType[_] =>
            buffer.append(prefix + param.name)
          case pt: PluginObjectParameterTypeTrait =>
            pt.pluginDescription match {
              case Some(pd) =>
                new ReconfigurablePluginDescription(pd).collectConfigProperties(buffer, prefix + param.name + "-")
              case None =>
                // Parameters not available
            }
        }
      }
    }
  }

  implicit class ReconfigurableTask[T <: TaskSpec](task: Task[T]) {

    /**
     * Retrieves the config port of this task.
     */
    def configPort: Port = {
      FixedSchemaPort(configSchema)
    }

    /**
     * Retrieves the schema of the config port of this task.
     */
    def configSchema: EntitySchema = {
      val pluginDesc = PluginDescription.forTask(task)
      EntitySchema(
        typeUri = "",
        typedPaths = for(property <- pluginDesc.configProperties) yield UntypedPath(property).asStringTypedPath
      )
    }

    /**
     * Reconfigures a task based on entity values.
     * Parameter templates that are not overwritten resolve as at load time, i.e. against the task's own execution variables.
     *
     * @param entities Task parameters will be updated based on the values in this entity.
     *                 Config parameters of later entities overwrite those of earlier inputs.
     * @param project  The project of the task.
     * @return Task with updated parameters.
     */
    def reconfigure(entities: Seq[Entity], project: ProjectTrait)
                   (implicit user: UserContext): Task[T] = {
      implicit val pluginContext: PluginContext = PluginContext.fromTask(task, project)
      val parameters = task.data.parameters
      val configParameters = entities.
        map(entityToParameterValues(parameters, _)).
        foldLeft(ParameterValues.empty)(_ merge _)
      if (configParameters.values.isEmpty) {
        task
      } else {
        PlainTask(id = task.id, data = task.data.withParameters(configParameters, dropExistingValues = true), metaData = task.metaData).asInstanceOf[Task[T]]
      }
    }

    private def entityToParameterValues(parameterValues: ParameterValues, entity: Entity, prefix: String = ""): ParameterValues = {
      val updatedValues =
        for ((name, value) <- parameterValues.values) yield {
          value match {
            case values: ParameterValues =>
              name -> entityToParameterValues(values, entity, name + "-")
            case _ =>
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
          }
        }
      ParameterValues(updatedValues)
    }
  }
}


