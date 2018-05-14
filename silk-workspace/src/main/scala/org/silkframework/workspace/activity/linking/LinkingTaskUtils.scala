package org.silkframework.workspace.activity.linking

import org.silkframework.dataset.DatasetSpec.PlainDatasetSpec
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec}
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformSpec}
import org.silkframework.util.{DPair, Identifier, Uri}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.transform.TransformTaskUtils._

/**
  * Adds additional methods to linking tasks.
  */
object LinkingTaskUtils {

  implicit class LinkingTask(task: ProjectTask[LinkSpec]) {

    /**
      * Retrieves both data sources for this linking task.
      */
    def dataSources: DPair[DataSource] = {
      task.data.dataSelections.map(dataSource)
    }

    /**
      * Retrieves a specific data source for this linking task.
      */
    def dataSource(selection: DatasetSelection): DataSource = {
      task.project.taskOption[TransformSpec](selection.inputId) match {
        case Some(transformTask) =>
          transformTask.asDataSource(selection.typeUri)
        case None =>
          task.project.task[PlainDatasetSpec](selection.inputId).data.source
      }
    }

    /**
      * Retrieves all link sinks for this linking task.
      */
    def linkSinks = {
      task.data.outputs.flatMap(o => task.project.taskOption[DatasetSpec[Dataset]](o)).map(_.data.linkSink)
    }
  }

}
