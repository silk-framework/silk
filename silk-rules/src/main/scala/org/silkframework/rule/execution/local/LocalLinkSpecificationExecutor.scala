package org.silkframework.rule.execution.local

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.{EntityTable, LinksTable, LocalExecution}
import org.silkframework.execution.{ExecutionReport, Executor}
import org.silkframework.rule.execution._
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.util.{DPair, Uri}

/**
  * Created on 7/20/16.
  */
class LocalLinkSpecificationExecutor extends Executor[LinkSpec, LocalExecution] {
  override def execute(task: Task[LinkSpec], inputs: Seq[EntityTable], outputSchema: Option[EntitySchema], execution: LocalExecution, context: ActivityContext[ExecutionReport]): Option[EntityTable] = {
    val linkSpec = updateSelection(task.data, inputs)
    val sources = DPair[DataSource](new EntitySource(inputs(0)), new EntitySource(inputs(1)))
    val output = execution.createInternalDataset(None) // TODO: Is this needed?
    val activity = new GenerateLinks(task.id, sources, linkSpec, Seq(output.linkSink))
    val linking = Activity(activity).startBlockingAndGetValue()
    context.value() = linking
    Some(LinksTable(linking.links, linkSpec.rule.linkType, PlainTask(task.id, linkSpec)))
  }

  private class EntitySource(table: EntityTable) extends DataSource {

    def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None): Traversable[Entity] = {
      table.entities
    }

    def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
      Seq.empty
    }
  }

  private def updateSelection(linkSpec: LinkSpec, inputs: Seq[EntityTable]): LinkSpec = {
    val sourceSelection = linkSpec.dataSelections.source.copy(inputId = inputs(0).task.id)
    val targetSelection = linkSpec.dataSelections.target.copy(inputId = inputs(1).task.id)
    linkSpec.copy(dataSelections = DPair(sourceSelection, targetSelection))
  }
}
