package controllers.transform.transformTask

import org.silkframework.dataset.DatasetCharacteristics
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.rule.{DatasetSelection, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.activity.dataset.DatasetUtils
import org.silkframework.workspace.{Project, ProjectTask}

/** Utility functions for transform tasks. */
object TransformUtils {
  /** Returns the dataset characteristics if the input task of the transformation is a dataset. */
  def datasetCharacteristics(task: ProjectTask[TransformSpec])
                            (implicit userContext: UserContext): Option[DatasetCharacteristics] = {
    DatasetUtils.datasetCharacteristics(task.project, task.selection)
  }

  def isRdfInput(transformTask: ProjectTask[TransformSpec])
                (implicit userContext: UserContext): Boolean = {
    DatasetUtils.isRdfInput(transformTask.project, transformTask.selection)
    transformTask.project.taskOption[GenericDatasetSpec](transformTask.selection.inputId).exists(_.data.plugin.isInstanceOf[RdfDataset])
  }
}
