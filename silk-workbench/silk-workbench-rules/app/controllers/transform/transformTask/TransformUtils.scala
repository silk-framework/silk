package controllers.transform.transformTask

import org.silkframework.dataset.DatasetCharacteristics
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.ProjectTask

/** Utility functions for transform tasks. */
object TransformUtils {
  /** Returns the dataset characteristics if the input task of the transformation is a datset. */
  def datasetCharacteristics(task: ProjectTask[TransformSpec])
                            (implicit userContext: UserContext): Option[DatasetCharacteristics] = {
    task.project.taskOption[GenericDatasetSpec](task.selection.inputId)
      .map(_.data.characteristics)
  }

  def isRdfInput(transformTask: ProjectTask[TransformSpec])
                (implicit userContext: UserContext): Boolean = {
    transformTask.project.taskOption[GenericDatasetSpec](transformTask.selection.inputId).exists(_.data.plugin.isInstanceOf[RdfDataset])
  }
}
