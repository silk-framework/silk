package org.silkframework.rule.plugins.transformer.metadata

import org.silkframework.dataset.{DatasetSpec, ResourceBasedDataset}
import org.silkframework.rule.TaskContext
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.{Resource, ResourceManager}
import org.silkframework.runtime.validation.ValidationException


@Plugin(
  id = "inputFileAttributes",
  categories = Array("Metadata"),
  label = "Input file attributes",
  description = "Retrieves a metadata attribute from the input file (such as the file name)."
)
case class InputFileAttributesTransformer(@Param("File attribute to be retrieved from the input dataset.")
                                          attribute: FileAttributeEnum = FileAttributeEnum.name) extends Transformer {


  override def withContext(taskContext: TaskContext): Transformer = {
    taskContext.inputTasks.headOption.map(_.data) match {
      case Some(DatasetSpec(ds: ResourceBasedDataset, _, _)) =>
        ConstantTransformer(getAttribute(ds.file, taskContext.pluginContext.resources))
      case _ =>
        this
    }
  }

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    throw new ValidationException("No resource supplied by input dataset.")
  }

  private def getAttribute(file: Resource, resourceManager: ResourceManager): String = {
    attribute match {
      case FileAttributeEnum.name => file.name
      case FileAttributeEnum.relativePath => file.relativePath(resourceManager)
      case FileAttributeEnum.absolutePath => file.path
      case FileAttributeEnum.size => file.size.map(_.toString).getOrElse("")
      case FileAttributeEnum.modified => file.modificationTime.map(_.toString).getOrElse("")
    }
  }
}
