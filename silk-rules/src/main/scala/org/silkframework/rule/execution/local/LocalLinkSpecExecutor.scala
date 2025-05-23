package org.silkframework.rule.execution.local

import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec, EmptyDataset}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.TypedPath
import org.silkframework.execution.local._
import org.silkframework.execution.typed.LinksEntitySchema
import org.silkframework.execution.{EntityHolder, ExecutionReport, Executor, ExecutorOutput}
import org.silkframework.rule.LinkSpec.{MAX_LINK_LIMIT, MAX_LINK_LIMIT_CONFIG_KEY}
import org.silkframework.rule.execution._
import org.silkframework.rule.{LinkSpec, RuntimeLinkingConfig, TaskContext}
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{DPair, Uri}

/**
  * Local linking task executor
  */
class LocalLinkSpecExecutor extends Executor[LinkSpec, LocalExecution] {

  override def execute(task: Task[LinkSpec],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    assert(inputs.size == 2, "LinkSpecificationExecutor did not receive exactly two inputs (source, target).")

    implicit val prefixes: Prefixes = pluginContext.prefixes
    implicit val user: UserContext = pluginContext.user

    val linkSpec = updateSelection(task.data, inputs.head, inputs.tail.head)
    val sources = DPair[DataSource](
      entitySource(inputs.head, task.dataSelections.source.typeUri),
      entitySource(inputs.tail.head, task.dataSelections.target.typeUri)
    )
    val adaptedLinkLimit = LinkSpec.adaptLinkLimit(task.linkLimit)
    val linkConfig = RuntimeLinkingConfig(
      linkLimit = Some(adaptedLinkLimit),
      executionTimeout = Some(task.matchingExecutionTimeout * 1000L).filter(_ > 0)
    )
    val taskContext = TaskContext(inputs.map(_.task), pluginContext)
    val activity = new GenerateLinks(task, sources, None, linkConfig, Some(task.data.rule.withContext(taskContext)))
    var linking = context.child(activity, progressContribution = 1.0).startBlockingAndGetValue()
    if(adaptedLinkLimit < task.linkLimit) {
      linking = linking.copy(matcherWarnings = linking.matcherWarnings ++ Seq(
        s"The link limit has been decreased to $MAX_LINK_LIMIT according to the configuration of '$MAX_LINK_LIMIT_CONFIG_KEY', since the" +
          s" value for this linking task has been set to a higher value: ${task.linkLimit}."
      ))
    }
    context.value() = linking
    Some(LinksEntitySchema.create(linking.links, task))
  }

  private def entitySource(input: LocalEntities, typeUri: Uri): EntitySource = {
    input match {
      case mt: MultiEntityTable if typeUri.uri.nonEmpty =>
        mt.allTables.find(_.entitySchema.typeUri == typeUri) match {
          case Some(table) =>
            new EntitySource(table)
          case None =>
            throw new ValidationException(s"No input table for type $typeUri found.")
        }
      case table =>
        new EntitySource(table)
    }
  }

  private class EntitySource(table: LocalEntities) extends DataSource {

    override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None)
                         (implicit context: PluginContext): EntityHolder = {
      table
    }

    override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                              (implicit context: PluginContext): EntityHolder = {
      EmptyEntityTable(underlyingTask)
    }

    override def retrieveTypes(limit: Option[Int])
                              (implicit userContext: UserContext, prefixes: Prefixes): Iterable[(String, Double)] = Iterable.empty

    override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                              (implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = IndexedSeq.empty

    /**
      * The dataset task underlying the Datset this source belongs to
      *
      * @return
      */
    override lazy val underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(table.task.id, DatasetSpec(EmptyDataset))   //FIXME CMEM-1352 replace with table.task???
  }

  private def updateSelection(linkSpec: LinkSpec, source: LocalEntities, target: LocalEntities): LinkSpec = {
    val sourceSelection = linkSpec.dataSelections.source.copy(inputId = source.task.id)
    val targetSelection = linkSpec.dataSelections.target.copy(inputId = target.task.id)
    linkSpec.copy(source = sourceSelection, target = targetSelection)
  }
}
