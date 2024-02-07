package org.silkframework.workspace.activity.linking

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec, EmptySource, LinkSink}
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.DPair
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
    def dataSources(implicit userContext: UserContext): DPair[DataSource] = {
      task.data.dataSelections.map(dataSource)
    }

    /**
      * Retrieves a specific data source for this linking task.
      */
    def dataSource(selection: DatasetSelection)
                  (implicit userContext: UserContext): DataSource = {
      task.project.taskOption[TransformSpec](selection.inputId) match {
        case Some(transformTask) =>
          transformTask.asDataSource(selection.typeUri)
        case None =>
          task.project.taskOption[GenericDatasetSpec](selection.inputId)
            .map(_.data.source)
            // Only datasets and transform inputs supported, everything else will be empty.
            .getOrElse(EmptySource)
      }
    }

    /**
      * Retrieves all link sinks for this linking task.
      */
    def linkSink(implicit userContext: UserContext): Option[LinkSink] = {
      task.data.output.flatMap(o => task.project.taskOption[DatasetSpec[Dataset]](o)).map(_.data.linkSink)
    }
  }

}
