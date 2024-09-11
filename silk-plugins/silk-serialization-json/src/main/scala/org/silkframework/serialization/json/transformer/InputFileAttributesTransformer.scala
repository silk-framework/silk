package org.silkframework.serialization.json.transformer

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.{DatasetSpec, ResourceBasedDataset}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.serialization.json.ResourceSerializers
import org.silkframework.workspace.ProjectTrait
import play.api.libs.json.JsValue


@Plugin(
  id = "inputFileAttributes",
  categories = Array("Dataset"),
  label = "Input file attributes",
  description = "Retrieves a metadata attribute from the input file (such as the file name) or the entire metadata as JSON."
)
case class InputFileAttributesTransformer(@Param("Path to retrieve from the JSON, such as 'name'. If left empty, the entire JSON will be returned.")
                                          path: String = "") extends JsonTransformer {

  def getJson(inputTask: Task[_ <: TaskSpec], project: ProjectTrait)
             (implicit pluginContext: PluginContext): JsValue = {
    inputTask.data match {
      case datasetSpec: DatasetSpec[_] =>
        datasetSpec.plugin match {
          case dataset: ResourceBasedDataset =>
            ResourceSerializers.resourceProperties(dataset.file, project.resources)
          case _ =>
            throw new ValidationException("The input dataset is not based on a file.")
        }
      case _ =>
        throw new ValidationException("The input is not a dataset")
    }

  }
}
