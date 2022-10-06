package controllers.transform.transformTask

import org.silkframework.dataset.DatasetCharacteristics
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.rule.{DatasetSelection, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.{Project, ProjectTask}

/** Utility functions for transform tasks. */
object TransformUtils {
  /** Returns the dataset characteristics if the input task of the transformation is a dataset. */
  def datasetCharacteristics(task: ProjectTask[TransformSpec])
                            (implicit userContext: UserContext): Option[DatasetCharacteristics] = {
    datasetCharacteristics(task.project, task.selection)
  }

  /** Returns the dataset characteristics of the dataset selection. */
  def datasetCharacteristics(project: Project,
                             datasetSelection: DatasetSelection)
                            (implicit userContext: UserContext): Option[DatasetCharacteristics] = {
    project.taskOption[GenericDatasetSpec](datasetSelection.inputId)
      .map(_.data.characteristics)
  }

  def isRdfInput(project: Project,
                 datasetSelection: DatasetSelection)
                (implicit userContext: UserContext): Boolean = {
    project.taskOption[GenericDatasetSpec](datasetSelection.inputId).exists(_.data.plugin.isInstanceOf[RdfDataset])
  }

  def isRdfInput(transformTask: ProjectTask[TransformSpec])
                (implicit userContext: UserContext): Boolean = {
    isRdfInput(transformTask.project, transformTask.selection)
    transformTask.project.taskOption[GenericDatasetSpec](transformTask.selection.inputId).exists(_.data.plugin.isInstanceOf[RdfDataset])
  }
}
