package org.silkframework.workspace.activity.transform

import org.silkframework.config.CustomTask
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec, EntitySink}
import org.silkframework.execution.TaskException
import org.silkframework.rule.{TransformSpec, TransformedDataSource}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri
import org.silkframework.workspace.ProjectTask
import org.silkframework.runtime.validation.ValidationException

/**
  * Adds additional methods to transform tasks.
  */
object TransformTaskUtils {

  implicit class TransformTask(task: ProjectTask[TransformSpec]) {

    /**
      * Retrieves the data source for this transform task.
      */
    def dataSource(implicit userContext: UserContext): DataSource = {
      val sourceId = task.data.selection.inputId
      task.project.taskOption[CustomTask](sourceId) match {
        case Some(customTask) =>
          throw TaskException(s"Task ${customTask.id} of type 'Other' is not supported as data source. Evaluate and Execute actions are thus not working.")
        case None =>
          task.project.taskOption[TransformSpec](sourceId) match {
            case Some(transformTask) =>
              transformTask.asDataSource(transformTask.data.selection.typeUri)
            case None =>
              task.project.task[GenericDatasetSpec](sourceId).data.source
          }
      }
    }

    /**
      * Converts this transform task to a data source.
      */
    def asDataSource(typeUri: Uri)
                    (implicit userContext: UserContext): DataSource = {
      val transformSpec = task.data
      val source = task.project.task[GenericDatasetSpec](transformSpec.selection.inputId).data.source

      // Find the rule that generates the selected type
      if(typeUri.uri.isEmpty) {
        new TransformedDataSource(source, transformSpec.inputSchema, transformSpec.mappingRule)
      } else {
        transformSpec.ruleSchemata.find(_.transformRule.rules.typeRules.map(_.typeUri).contains(typeUri)) match {
          case Some(ruleSchemata) =>
            new TransformedDataSource(source, ruleSchemata.inputSchema, ruleSchemata.transformRule)
          case None =>
            throw new ValidationException(s"No rule matching target type $typeUri found.")
        }
      }
    }

    /**
      * Retrieves all entity sinks for this transform task.
      */
    def entitySink(implicit userContext: UserContext): Option[EntitySink] = {
      task.data.output.flatMap(o => task.project.taskOption[GenericDatasetSpec](o)).map(_.data.entitySink)
    }

    /**
      * Retrieves all error entity sinks for this transform task.
      */
    def errorEntitySink(implicit userContext: UserContext): Option[EntitySink] = {
      task.data.errorOutput.flatMap(o => task.project.taskOption[GenericDatasetSpec](o)).map(_.data.entitySink)
    }
  }

}
