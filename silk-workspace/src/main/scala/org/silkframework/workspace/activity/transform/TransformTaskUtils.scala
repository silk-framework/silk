package org.silkframework.workspace.activity.transform

import org.silkframework.dataset.{DataSource, Dataset, DatasetTask}
import org.silkframework.rule.{TransformSpec, TransformedDataSource}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.workspace.ProjectTask

/**
  * Adds additional methods to transform tasks.
  */
object TransformTaskUtils {

  implicit class TransformTask(task: ProjectTask[TransformSpec]) {

    /**
      * Retrieves the data source for this transform task.
      */
    def dataSource: DataSource = {
      val sourceId = task.data.selection.inputId
      task.project.taskOption[TransformSpec](sourceId) match {
        case Some(transformTask) =>
          val transformSpec = transformTask.data
          val source = task.project.task[Dataset](transformSpec.selection.inputId).data.source

          // Find the rule that generates the selected type
          val typeUri = transformSpec.selection.typeUri
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
        case None =>
          task.project.task[Dataset](sourceId).data.source
      }
    }

    /**
      * Retrieves all entity sinks for this transform task.
      */
    def entitySinks = {
      task.data.outputs.flatMap(o => task.project.taskOption[Dataset](o)).map(_.data.entitySink)
    }

    /**
      * Retrieves all error entity sinks for this transform task.
      */
    def errorEntitySinks = {
      task.data.errorOutputs.flatMap(o => task.project.taskOption[Dataset](o)).map(_.data.entitySink)
    }
  }

}
