package org.silkframework.rule.execution.local

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.execution.local.{LocalEntities, LinksTable, LocalExecution, MultiEntityTable}
import org.silkframework.execution.{ExecutionReport, Executor}
import org.silkframework.rule.execution._
import org.silkframework.rule.{LinkSpec, TransformSpec, TransformedDataSource}
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{DPair, Uri}

/**
  * Created on 7/20/16.
  */
class LocalLinkSpecificationExecutor extends Executor[LinkSpec, LocalExecution] {

  override def execute(task: Task[LinkSpec],
                       inputs: Seq[LocalEntities],
                       outputSchema: Option[EntitySchema],
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport]
                      ): Option[LocalEntities] = {
    assert(inputs.size == 2, "LinkSpecificationExecutor did npt receive exactly two inputs (source, target).")
    val linkSpec = updateSelection(task.data, inputs.head, inputs.tail.head)
    val sources = DPair[DataSource](
      entitySource(inputs.head, task.dataSelections.source.typeUri),
      entitySource(inputs.tail.head, task.dataSelections.target.typeUri)
    )
    val output = execution.createInternalDataset(None) // TODO: Is this needed?
    val activity = new GenerateLinks(task.id, sources, linkSpec, Seq(output.linkSink))
    val linking = Activity(activity).startBlockingAndGetValue()
    context.value() = linking
    Some(LinksTable(linking.links, linkSpec.rule.linkType, Some(PlainTask(task.id, linkSpec))))
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

    def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None): Traversable[Entity] = {
      table.entities
    }

    def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
      Seq.empty
    }

    override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = Traversable.empty

    override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int]): IndexedSeq[Path] = IndexedSeq.empty
  }

  private def updateSelection(linkSpec: LinkSpec, source: LocalEntities, target: LocalEntities): LinkSpec = {
    val sourceSelection = linkSpec.dataSelections.source.copy(inputId = source.taskOption.getOrElse(throw new IllegalArgumentException).id)
    val targetSelection = linkSpec.dataSelections.target.copy(inputId = target.taskOption.getOrElse(throw new IllegalArgumentException).id)
    linkSpec.copy(dataSelections = DPair(sourceSelection, targetSelection))
  }
}
