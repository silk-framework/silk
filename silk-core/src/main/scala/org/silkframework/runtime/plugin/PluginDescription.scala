package org.silkframework.runtime.plugin

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.runtime.plugin.StringParameterType.PasswordParameterType
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.Identifier

import scala.util.control.NonFatal

/**
  * Describes a plugin.
  *
  * @tparam T The plugin base class.
  */
trait PluginDescription[+T] {

  /** The id of this plugin */
  val id: Identifier

  /** The categories to which this plugin belongs to. */
  val categories: Seq[String]

  /** A human-readable label.  */
  val label: String

  /** A short (few sentence) description of this plugin. */
  val description: String

  /** Documentation for this plugin in Markdown. */
  val documentation: String

  /** The parameters of the plugin class.  */
  val parameters: Seq[PluginParameter]

  /** The concrete plugin class  */
  def pluginClass: Class[_ <: T]

  /** The plugin types for this plugin. Ideally just one. */
  val pluginTypes: Seq[PluginTypeDescription]

  /** The plugin icon as Data URL string. If the string is empty, a generic icon is used. */
  val icon: Option[String]

  /** The actions that can be performed on this plugin. */
  val actions: Seq[PluginAction]

  /** Custom plugin descriptions */
  lazy val customDescriptions: Seq[CustomPluginDescription] = pluginTypes.flatMap(_.customDescription.generate(pluginClass))

  /**
    * Creates an instance of this plugin with the given parameters.
    *
    * @param parameterValues The parameter values to be used for instantiation. Will override any default.
    * @param ignoreNonExistingParameters If true, parameter values for parameters that do not exist are ignored.
    *                                   If false, creation will fail if a parameter is provided that does not exist on the plugin.
    */
  def apply(parameterValues: ParameterValues = ParameterValues.empty,
            ignoreNonExistingParameters: Boolean = true)
           (implicit context: PluginContext): T


  /**
    * Retrieves the parameters values of a given plugin instance.
    */
  def parameterValues(plugin: AnyPlugin)(implicit pluginContext: PluginContext): ParameterValues = {
    val values =
      for(param <- parameters) yield {
        val value =
          param.parameterType match {
            case _: StringParameterType[_] =>
              plugin.templateValues.get(param.name) match {
                case Some(templateValue) =>
                  ParameterTemplateValue(templateValue)
                case None =>
                  ParameterStringValue(param.stringValue(plugin))
              }
            case pt: PluginObjectParameterTypeTrait =>
              pt.pluginDescription match {
                case Some(pd) =>
                  pd.parameterValues(param(plugin).asInstanceOf[AnyPlugin])
                case None =>
                  ParameterObjectValue(param(plugin))
              }
          }
        (param.name, value)
      }
    ParameterValues(values.toMap)
  }

  /**
    * Retrieves a parameter by its name.
    *
    * @throws NotFoundException If the no parameter with the given name has been found.
    */
  def findParameter(name: String): PluginParameter = {
    parameters.find(_.name == name) match {
      case Some(parameter) =>
        parameter
      case None =>
        throw new NotFoundException(s"Plugin '${id}' does not have a parameter '${name}'.")
    }
  }

  /**
    * Parses parameter values.
    */
  protected def parseParameters(parameterValues: ParameterValues)(implicit context: PluginContext): Seq[AnyRef] = {
    for (parameter <- parameters) yield {
      parameterValues.values.get(parameter.name) match {
        case Some(value) =>
          try {
            parameter.parameterType match {
              case stringParam: StringParameterType[_] =>
                parseStringParameter(stringParam, value)
              case objParam: PluginObjectParameterTypeTrait =>
                parseObjectParameter(objParam, value)
            }
          } catch {
            case NonFatal(ex) =>
              throw new InvalidPluginParameterValueException(s"Invalid value for plugin parameter '${parameter.name}' of plugin '$id'. ${ex.getMessage}", ex)
          }
        case None if parameter.defaultValue.isDefined =>
          parameter.defaultValue.get
        case None =>
          throw new InvalidPluginParameterValueException("Parameter '" + parameter.name + "' is required for " + label)
      }
    }
  }

  private def parseStringParameter(stringParam: StringParameterType[_], value: ParameterValue)
                                  (implicit context: PluginContext): AnyRef = {
    value match {
      case ParameterStringValue(strValue) =>
        try {
          stringParam.fromString(strValue).asInstanceOf[AnyRef]
        } catch {
          case NonFatal(ex) =>
            throw new InvalidPluginParameterValueException(s"Got '$strValue', but expected: ${stringParam.description.stripSuffix(".")}. Details: ${ex.getMessage}", ex)
        }
      case template: ParameterTemplateValue =>
        // Only password parameters are allowed to resolve sensitive variables
        val templateVariables =
          if(stringParam.name == PasswordParameterType.name) {
            context.templateVariables.all
          } else {
            context.templateVariables.all.withoutSensitiveVariables()
          }
        // Evaluate template
        val evaluatedValue = template.evaluate(templateVariables)
        try {
          stringParam.fromString(evaluatedValue).asInstanceOf[AnyRef]
        } catch {
          case NonFatal(ex) =>
            throw new InvalidPluginParameterValueException(s"Got '$evaluatedValue' based on template '${template.template}', " +
              s"but expected: ${stringParam.description.stripSuffix(".")}. Details: ${ex.getMessage}", ex)
        }
      case parameterObjectValue: ParameterObjectValue =>
        parameterObjectValue.value(context)
      case _ =>
        throw new IllegalArgumentException(s"Expected a string parameter, but got $value.")
    }
  }

  private def parseObjectParameter(objParam: PluginObjectParameterTypeTrait, value: ParameterValue)
                                  (implicit context: PluginContext): AnyRef = {
    value match {
      case parameterObjectValue: ParameterObjectValue =>
        parameterObjectValue.value(context)
      case values: ParameterValues =>
        objParam.pluginDescription match {
          case Some(pluginDesc: PluginDescription[_]) =>
            pluginDesc(values).asInstanceOf[AnyRef]
          case _ =>
            throw new IllegalArgumentException(s"No plugin description available. Value needs to be provided using a ${classOf[ParameterObjectValue].getClass.getSimpleName}.")
        }
      case _ =>
        throw new IllegalArgumentException(s"Expected a complex parameter, but got $value.")
    }
  }

}

object PluginDescription {

  /** Returns a plugin description for a given task. */
  def forTask(task: Task[_ <: TaskSpec]): PluginDescription[_] = {
    task.data match {
      case plugin: AnyPlugin => plugin.pluginSpec
      case DatasetSpec(plugin, _, _) => plugin.pluginSpec
      case _ => throw new IllegalArgumentException(s"Provided task $task is not a plugin.")
    }
  }

}