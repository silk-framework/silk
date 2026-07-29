package org.silkframework.workspace.activity.transform

import org.silkframework.config.FixedSchemaPort
import org.silkframework.dataset.{DataSource, DatasetCharacteristics}
import org.silkframework.dataset.DatasetCharacteristics.SupportedPathExpressions
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.ExecutorRegistry
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.{Identifier, Uri}
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

  /** The characteristics of the input, e.g., if it supports multi-hop paths. */
  def characteristics: DatasetCharacteristics
}

object ResolvedTransformInput {

  /** The input is a dataset, so the schema needs to be extracted from the data. */
  case class DatasetInput(datasetTask: ProjectTask[GenericDatasetSpec]) extends ResolvedTransformInput {
    override def datasetTaskOption: Option[ProjectTask[GenericDatasetSpec]] = Some(datasetTask)
    override def dataSourceOption(implicit userContext: UserContext): Option[DataSource] = {
      Some(ExecutorRegistry.access(datasetTask).source)
    }
    override def characteristics: DatasetCharacteristics = datasetTask.data.characteristics
  }

  /**
    * The input has a statically known schema, i.e., it is another transform task or a task with a fixed output schema.
    * For transform tasks whose input chain ends in a dataset, a data source is available that executes the transformation.
    *
    * @param typeUri      The type that is read from the upstream transform. Empty if its primary output type is read.
    * @param resolvedRule The upstream rule that generates the read entities, i.e. `schema` is its output schema.
    *                     Resolved once, so that schema, rule scoping and delivery stay consistent.
    */
  case class StaticSchemaInput(schema: EntitySchema,
                               upstreamTransform: Option[ProjectTask[TransformSpec]] = None,
                               typeUri: Uri = Uri(""),
                               resolvedRule: Option[RuleSchemata] = None) extends ResolvedTransformInput {
    override def dataSourceOption(implicit userContext: UserContext): Option[DataSource] = {
      dataSourceOfChain(Seq.empty)
    }

    /**
      * A data source that executes the upstream transform on its own input, which may be a transform task itself.
      *
      * @param visitedTasks The transform tasks already resolved in this chain. The workspace rejects circular inputs
      *                     only when a task is added or updated, so a project that is imported or loaded from the
      *                     workspace provider can still contain a cycle.
      * @throws BadUserInputException If the chain of transform tasks is circular.
      */
    private def dataSourceOfChain(visitedTasks: Seq[Identifier])
                                 (implicit userContext: UserContext): Option[DataSource] = {
      for {
        transformTask <- upstreamTransform
        inputSource <- inputSourceOfChain(transformTask, visitedTasks)
      } yield {
        transformTask.asDataSource(inputSource, typeUri)
      }
    }

    /** The data source that provides the input entities of the given transform task, which may be a transform itself. */
    private def inputSourceOfChain(transformTask: ProjectTask[TransformSpec], visitedTasks: Seq[Identifier])
                                  (implicit userContext: UserContext): Option[DataSource] = {
      val chain = visitedTasks :+ transformTask.id
      if(visitedTasks.contains(transformTask.id)) {
        throw new BadUserInputException(s"The inputs of transform task '${transformTask.id}' are circular: " +
          chain.mkString(" -> ") + ".")
      }
      val resolvedInput =
        try {
          Some(transformTask.resolveInput)
        } catch {
          // An input without a fixed output schema cannot provide data; a missing task is a misconfiguration
          case _: BadUserInputException => None
        }
      resolvedInput.flatMap {
        case upstreamInput: StaticSchemaInput => upstreamInput.dataSourceOfChain(chain)
        case input => input.dataSourceOption
      }
    }

    /**
      * The schemata of this input: the resolved rule and all rules nested below it, or the fixed schema itself.
      * Sub paths are relative to the resolved rule, which addresses the entities that the nested rules generate.
      */
    def nestedSchemata: Seq[EntitySchema] = {
      (upstreamTransform, resolvedRule) match {
        case (Some(upstream), Some(rule)) =>
          // Scoped by rule tree, not by path prefix: a sibling rule may share the target path, but is not delivered
          for(outputSchema <- upstream.data.ruleSchemataWithinRule(rule.transformRule).map(_.outputSchema)) yield {
            outputSchema.copy(subPath = UntypedPath.removePathPrefix(outputSchema.subPath, schema.subPath))
          }
        case _ =>
          Seq(schema)
      }
    }

    /** Nested object rules of a transform and multi-hop paths of a fixed output schema both make an input hierarchical. */
    override def characteristics: DatasetCharacteristics = {
      val multiHopPaths = nestedSchemata.exists(s => s.subPath.operators.nonEmpty || s.typedPaths.exists(_.operators.size > 1))
      DatasetCharacteristics(
        supportedPathExpressions = SupportedPathExpressions(multiHopPaths = multiHopPaths),
        supportsMultipleTables = false
      )
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
    val selection = transformTask.data.selection
    val inputId = selection.inputId
    project.taskOption[GenericDatasetSpec](inputId).map(DatasetInput)
      .orElse(project.taskOption[TransformSpec](inputId).map { upstream =>
        val typeUri = effectiveTypeUri(upstream, selection.typeUri)
        val ruleSchemata = upstream.data.ruleSchemataForTargetTypeOrPrimary(typeUri)
        StaticSchemaInput(ruleSchemata.outputSchema, Some(upstream), typeUri, Some(ruleSchemata))
      })
      .getOrElse {
        project.anyTask(inputId).data.outputPort match {
          case Some(FixedSchemaPort(schema)) =>
            StaticSchemaInput(schema)
          case _ =>
            throw new BadUserInputException(s"The input task '$inputId' of transform task '${transformTask.id}' does not provide a fixed output schema. " +
              "The input of a transform task must be a dataset, a transform task or a task with a fixed output schema.")
        }
      }
  }

  /**
    * The type that is read from the given transform task.
    * A selected type that none of its rules generates is ignored, e.g. one left over from a previous input dataset.
    * This is deliberate even for a type that matched before the upstream was edited: falling back to the primary
    * rule keeps the task usable, while an error would also block the editors needed to fix the selection.
    */
  private def effectiveTypeUri(transformTask: ProjectTask[TransformSpec], selectedType: Uri): Uri = {
    if(selectedType.uri.nonEmpty && transformTask.data.ruleSchemataForTargetTypeOption(selectedType).isDefined) {
      selectedType
    } else {
      Uri("")
    }
  }
}
