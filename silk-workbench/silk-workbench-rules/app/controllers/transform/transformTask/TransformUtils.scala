package controllers.transform.transformTask

import org.silkframework.dataset.DatasetCharacteristics
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.rule.{DatasetSelection, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.dataset.DatasetUtils
import org.silkframework.workspace.{Project, ProjectTask}

/** Utility functions for transform tasks. */
object TransformUtils {
  /** Returns the dataset characteristics of the input task of the transformation is a dataset. */
  def datasetCharacteristics(task: ProjectTask[TransformSpec])
                            (implicit userContext: UserContext): Option[DatasetCharacteristics] = {
    datasetCharacteristics(task.project, task.selection)
  }

  /** Returns the dataset characteristics of the dataset selection. */
  def datasetCharacteristics(project: Project,
                             datasetSelection: DatasetSelection)
                            (implicit userContext: UserContext): Option[DatasetCharacteristics] = {
    DatasetUtils.datasetCharacteristics(project, datasetSelection)
  }

  /** Returns true if the dataset selection is a RDF dataset. */
  def isRdfDataset(project: Project,
                   datasetSelection: DatasetSelection)
                  (implicit userContext: UserContext): Boolean = {
    isRdfDatasetById(project, datasetSelection.inputId)
  }

  private def isRdfDatasetById(project: Project,
                               datasetId: Identifier)
                              (implicit userContext: UserContext): Boolean = {
    project.taskOption[GenericDatasetSpec](datasetId).exists(_.data.plugin.isInstanceOf[RdfDataset])
  }

  /** Returns the dataset characteristics of the dataset selection. */
  def outputDatasetCharacteristics(transformTask: ProjectTask[TransformSpec])
                            (implicit userContext: UserContext): Option[DatasetCharacteristics] = {
    transformTask.output.value.flatMap(output => datasetCharacteristics(transformTask.project, DatasetSelection(output)))
  }

  /** Returns true if the configured input task is a RDF dataset. */
  def isRdfInput(transformTask: ProjectTask[TransformSpec])
                (implicit userContext: UserContext): Boolean = {
    DatasetUtils.isRdfInput(transformTask.project, transformTask.selection)
  }

  /** Returns true if the configured output task is a RDF dataset. */
  def isRdfOutput(transformTask: ProjectTask[TransformSpec])
                 (implicit userContext: UserContext): Boolean = {
    transformTask.output.value match {
      case Some(output) =>
        isRdfDatasetById(transformTask.project, output)
      case None => false
    }
  }
}
