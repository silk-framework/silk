package org.silkframework.rule.execution.local

import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec, EmptyDataset}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.{EmptyEntityTable, LinksTable, LocalEntities, LocalExecution, MultiEntityTable}
import org.silkframework.execution.{EntityHolder, ExecutionReport, Executor, ExecutorOutput}
import org.silkframework.rule.{LinkSpec, RuntimeLinkingConfig}
import org.silkframework.rule.execution._
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
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
                      (implicit userContext: UserContext, prefixes: Prefixes): Option[LocalEntities] = {
    assert(inputs.size == 2, "LinkSpecificationExecutor did not receive exactly two inputs (source, target).")
    val linkSpec = updateSelection(task.data, inputs.head, inputs.tail.head)
    val sources = DPair[DataSource](
      entitySource(inputs.head, task.dataSelections.source.typeUri),
      entitySource(inputs.tail.head, task.dataSelections.target.typeUri)
    )
    val linkConfig = RuntimeLinkingConfig(
      linkLimit = Some(LinkSpec.adaptLinkLimit(task.linkLimit)),
      executionTimeout = Some(task.matchingExecutionTimeout * 1000L).filter(_ > 0)
    )
    val activity = new GenerateLinks(task.id, task.taskLabel(), sources, linkSpec, None, linkConfig)
    val linking = context.child(activity).startBlockingAndGetValue()
    context.value() = linking
    Some(LinksTable(linking.links, linkSpec.rule.linkType, task))
  }

  private def entitySource(input: LocalEntities, typeUri: Uri): EntitySource = {
    input match {
      case mt: MultiEntityTable if typeUri.uri.nonEmpty =>
        val allTables = mt +: mt.subTables
        allTables.find(_.entitySchema.typeUri == typeUri) match {
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
                         (implicit userContext: UserContext): EntityHolder = {
      table
    }

    override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                              (implicit userContext: UserContext): EntityHolder = {
      EmptyEntityTable(underlyingTask)
    }

    override def retrieveTypes(limit: Option[Int])
                              (implicit userContext: UserContext): Traversable[(String, Double)] = Traversable.empty

    override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                              (implicit userContext: UserContext): IndexedSeq[TypedPath] = IndexedSeq.empty

    /**
      * The dataset task underlying the Datset this source belongs to
      *
      * @return
      */
    override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(table.task.id, DatasetSpec(EmptyDataset))   //FIXME CMEM-1352 replace with table.task???
  }

  private def updateSelection(linkSpec: LinkSpec, source: LocalEntities, target: LocalEntities): LinkSpec = {
    val sourceSelection = linkSpec.dataSelections.source.copy(inputId = source.task.id)
    val targetSelection = linkSpec.dataSelections.target.copy(inputId = target.task.id)
    linkSpec.copy(source = sourceSelection, target = targetSelection)
  }
}
