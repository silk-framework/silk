package org.silkframework.rule.plugins.transformer.dataset

import org.silkframework.rule.TaskContext
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.runtime.plugin.ParameterValueUtils.ExtendedParameterValues
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.validation.ValidationException


@Plugin(
  id = "inputTaskParameter",
  categories = Array("Dataset"),
  label = "Input Task parameter",
  description = "Retrieves a parameter of the input task."
)
case class InputTaskParameterTransformer(parameter: String) extends Transformer {

  override def withContext(taskContext: TaskContext): Transformer = {
    implicit val pluginContext: PluginContext = taskContext.pluginContext
    taskContext.inputTasks.headOption match {
      case Some(inputTask) =>
        inputTask.data.parameters.valueAtPath(parameter) match {
          case Some(value) =>
            ConstantTransformer(value)
          case None =>
            throw new ValidationException(s"The parameter path '$parameter' is not valid. Available paths: ${inputTask.data.parameters.availablePaths.mkString(", ")}")
        }
      case None =>
        this
    }
  }

  def apply(values: Seq[Seq[String]]): Seq[String] = {
    throw new ValidationException("No input task available.")
  }
}