package org.silkframework.workspace.activity.transform

import org.silkframework.config.FixedSchemaPort
import org.silkframework.dataset.DataSource
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.ExecutorRegistry
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.transform.TransformTaskUtils._

/**
  * The resolved input of a transform task.
  * The input may be a dataset, another transform task or any task with a fixed output schema.
  */
sealed trait ResolvedTransformInput {

  /** The input dataset task, if the input is a dataset. */
  def datasetTaskOption: Option[ProjectTask[GenericDatasetSpec]] = None

  /** A data source for retrieving input entities, if the input can provide data without executing a workflow. */
  def dataSourceOption(implicit userContext: UserContext): Option[DataSource]
}

object ResolvedTransformInput {

  /** The input is a dataset, so the schema needs to be extracted from the data. */
  case class DatasetInput(datasetTask: ProjectTask[GenericDatasetSpec]) extends ResolvedTransformInput {
    override def datasetTaskOption: Option[ProjectTask[GenericDatasetSpec]] = Some(datasetTask)
    override def dataSourceOption(implicit userContext: UserContext): Option[DataSource] = {
      Some(ExecutorRegistry.access(datasetTask).source)
    }
  }

  /**
    * The input has a statically known schema, i.e., it is another transform task or a task with a fixed output schema.
    * For transform tasks that read from a dataset, a data source is available that executes the transformation.
    */
  case class StaticSchemaInput(schema: EntitySchema,
                               upstreamTransform: Option[ProjectTask[TransformSpec]] = None) extends ResolvedTransformInput {
    override def dataSourceOption(implicit userContext: UserContext): Option[DataSource] = {
      // Executing the upstream transform requires its own input to be a dataset
      upstreamTransform.filter(_.inputDatasetTaskOption.isDefined)
        .map(transformTask => transformTask.asDataSource(transformTask.data.selection.typeUri))
    }
  }

  /**
    * Resolves the input of a transform task.
    *
    * @throws BadUserInputException If the input task does not provide a fixed output schema.
    * @throws org.silkframework.workspace.exceptions.TaskNotFoundException If the input task does not exist.
    */
  def resolve(transformTask: ProjectTask[TransformSpec])
             (implicit userContext: UserContext): ResolvedTransformInput = {
    val project = transformTask.project
    val inputId = transformTask.data.selection.inputId
    project.taskOption[GenericDatasetSpec](inputId).map(DatasetInput) match {
      case Some(datasetInput) =>
        datasetInput
      case None =>
        project.taskOption[TransformSpec](inputId) match {
          case Some(upstream) =>
            StaticSchemaInput(outputSchema(upstream), Some(upstream))
          case None =>
            project.anyTask(inputId).data.outputPort match {
              case Some(FixedSchemaPort(schema)) =>
                StaticSchemaInput(schema)
              case _ =>
                throw new BadUserInputException(s"The input task '$inputId' of transform task '${transformTask.id}' does not provide a fixed output schema. " +
                  "The input of a transform task must be a dataset, a transform task or a task with a fixed output schema.")
            }
        }
    }
  }

  /**
    * The output schema of the rule that generates the selected type of the given transform task.
    * Selects the same rule as TransformTaskUtils.asDataSource.
    */
  private def outputSchema(transformTask: ProjectTask[TransformSpec]): EntitySchema = {
    val transformSpec = transformTask.data
    val typeUri = transformSpec.selection.typeUri
    if (typeUri.uri.isEmpty) {
      transformSpec.ruleSchemataWithoutEmptyObjectRules.head.outputSchema
    } else {
      transformSpec.ruleSchemataForTargetType(typeUri).outputSchema
    }
  }
}
