package org.silkframework.workspace.activity.transform

import org.silkframework.config.TransformSpecification
import org.silkframework.dataset.{DataSource, Dataset}
import org.silkframework.workspace.Task

/**
  * Adds additional methods to transform tasks.
  */
object TransformTaskUtils {

  implicit class TransformTask(task: Task[TransformSpecification]) {

    /**
      * Retrieves the data source for this transform task.
      */
    def dataSource: DataSource = {
      task.project.task[Dataset](task.data.selection.inputId).data.source
    }

    /**
      * Retrieves all entity sinks for this transform task.
      */
    def entitySinks = {
      task.data.outputs.flatMap(o => task.project.taskOption[Dataset](o)).map(_.data.entitySink)
    }
  }

}
