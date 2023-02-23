/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.runtime.plugin

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.ResourceNotFoundException
import org.silkframework.runtime.templating.GlobalTemplateVariables
import org.silkframework.util.Identifier
import org.silkframework.util.StringUtils._
import org.silkframework.workspace.WorkspaceReadTrait

import java.lang.reflect.{Constructor, InvocationTargetException, Type}
import scala.io.{Codec, Source}
import scala.language.existentials
import scala.util.control.NonFatal

/**
  * Describes a plugin that is based on a Scala class.
  *
  * @param id The id of this plugin.
  * @param categories The categories to which this plugin belongs to.
  * @param label A human-readable label.
  * @param description A short (few sentence) description of this plugin.
  * @param documentation Documentation for this plugin in Markdown.
  * @param parameters The parameters of the plugin class.
  * @param constructor The constructor for creating a new instance of this plugin.
  * @tparam T The class that implements this plugin.
  */
class ClassPluginDescription[+T <: AnyPlugin](val id: Identifier, val categories: Seq[String], val label: String, val description: String,
                                              val documentation: String, val parameters: Seq[ClassPluginParameter], constructor: Constructor[T]) extends PluginDescription[T] {

  /**
    * The plugin class.
    */
  def pluginClass: Class[_ <: T] = {
    constructor.getDeclaringClass
  }

  /**
   * Creates a new instance of this plugin.
   *
   * @param parameterValues The parameter values to be used for instantiation. Will override any default.
   * @param ignoreNonExistingParameters If true, parameter values for parameters that do not exist are ignored.
    *                                   If false, creation will fail if a parameter is provided that does not exist on the plugin.
   */
  def apply(parameterValues: ParameterValues = ParameterValues.empty,
            ignoreNonExistingParameters: Boolean = true)
           (implicit context: PluginContext): T = {
    if (!ignoreNonExistingParameters) {
      validateParameters(parameterValues)
    }
    val parsedParameters = parseParameters(parameterValues)
    try {
      val plugin = constructor.newInstance(parsedParameters: _*)
      plugin.templateValues = parameterValues.templates
      plugin
    } catch {
      case ex: InvocationTargetException => throw ex.getCause
    }
  }

  private def parseParameters(parameterValues: ParameterValues)(implicit context: PluginContext): Seq[AnyRef] = {
    for (parameter <- parameters) yield {
      parameterValues.values.get(parameter.name) match {
        case Some(v) =>
          try {
            parameter.parameterType match {
              case stringParam: StringParameterType[_] =>
                v match {
                  case ParameterStringValue(value) =>
                    stringParam.fromString(value).asInstanceOf[AnyRef]
                  case template: ParameterTemplateValue =>
                    stringParam.fromString(template.evaluate()).asInstanceOf[AnyRef]
                  case _ =>
                    // TODO
                    throw new IllegalArgumentException("")
                }
              case objParam: PluginObjectParameterTypeTrait =>
                v match {
                  case ParameterObjectValue(obj) =>
                    obj
                  case values: ParameterValues =>
                    objParam.pluginDescription match {
                      case Some(pluginDesc: PluginDescription[_]) =>
                        // TODO conversion
                        pluginDesc.asInstanceOf[ClassPluginDescription[_]](values).asInstanceOf[AnyRef]
                      case _ =>
                        throw new RuntimeException(s"No plugin description available for parameter ${parameter.name} of plugin $id.")
                    }
                  case _ =>
                    // TODO message still correct?
                    throw new RuntimeException(s"Plugin parameter '${parameter.name}' of plugin '$id' has no simple string representation, but is a complex object.") // TODO: What to do with that?
                }

            }
          } catch {
            case NonFatal(ex) =>
              throw new InvalidPluginParameterValueException(s"$label has an invalid value for parameter '${parameter.name}'. Value must be a valid " + parameter.parameterType + ". Issue: " + ex.getMessage, ex)
          }
        case None if parameter.defaultValue.isDefined =>
          parameter.defaultValue.get
        case None =>
          throw new InvalidPluginParameterValueException("Parameter '" + parameter.name + "' is required for " + label)
      }
    }
  }

  override def toString = label

  /**
    * Throws an exception if a parameter value is provided that does not exist on this plugin.
    */
  private def validateParameters(parameterValues: ParameterValues): Unit = {
    val invalidParameters = parameterValues.values.keySet -- parameters.map(_.name)
    if (invalidParameters.nonEmpty) {
      throw new InvalidPluginParameterValueException(s"The following parameters cannot be set on plugin '$label' because they are no valid parameters:" +
        s" ${invalidParameters.mkString(", ")}. Valid parameters are: ${parameters.map(_.name).mkString(", ")}")
    }
  }

}

/**
 * Factory for plugin description.
 */
object ClassPluginDescription {

  /**
    * Returns a plugin description for a given class.
    * If available, returns an already registered plugin description.
    */
  def apply[T <: AnyPlugin](pluginClass: Class[T]): PluginDescription[T] = {
    PluginRegistry.pluginDescription(pluginClass).getOrElse(create(pluginClass))
  }

  /**
    * Creates a new plugin description from a class.
    */
  def create[T <: AnyPlugin](pluginClass: Class[T]): ClassPluginDescription[T] = {
    getAnnotation(pluginClass) match {
      case Some(annotation) => createFromAnnotation(pluginClass, annotation)
      case None => createFromClass(pluginClass)
    }
  }

  private def getAnnotation[T](pluginClass: Class[T]): Option[Plugin] = {
    Option(pluginClass.getAnnotation(classOf[Plugin]))
  }

  private def createFromAnnotation[T <: AnyPlugin](pluginClass: Class[T], annotation: Plugin): ClassPluginDescription[T] = {
    val markdownDocString = loadMarkdownDocumentation(pluginClass, annotation.documentationFile)
    new ClassPluginDescription(
      id = annotation.id,
      label = annotation.label,
      categories = annotation.categories,
      description = annotation.description.stripMargin,
      documentation = markdownDocString + (if(markdownDocString.nonEmpty) "\n" else "") + addTransformDocumentation(pluginClass),
      parameters = getParameters(pluginClass),
      constructor = getConstructor(pluginClass)
    )
  }

  private def createFromClass[T <: AnyPlugin](pluginClass: Class[T]): ClassPluginDescription[T] = {
    new ClassPluginDescription(
      id = Identifier.fromAllowed(pluginClass.getSimpleName),
      label = pluginClass.getSimpleName,
      categories = Seq(PluginCategories.uncategorized),
      description = "",
      documentation = addTransformDocumentation(pluginClass),
      parameters = getParameters(pluginClass),
      constructor = getConstructor(pluginClass)
    )
  }

  private def loadMarkdownDocumentation(pluginClass: Class[_], classpath: String): String = {
    if(classpath.trim.isEmpty) {
      ""
    } else {
      val inputStream = pluginClass.getResourceAsStream(classpath)
      if (inputStream == null) {
        throw new ResourceNotFoundException(s"The documentation file for plugin $pluginClass has not been found at '$classpath'.")
      }
      val source = Source.fromInputStream(inputStream)(Codec.UTF8)
      try {
        source.getLines.mkString("\n")
      } finally {
        source.close()
      }
    }
  }

  private def addTransformDocumentation(pluginClass: Class[_]) = {
    val sb = new StringBuilder()

    val transformExamples = TransformExampleValue.retrieve(pluginClass)
    if(transformExamples.nonEmpty) {
      sb ++= "### Examples"
      sb ++= "\n\n"
      sb ++= "#### Notation\n\n"
      sb ++= "List of values are represented via square brackets. Example: `[first, second]` represents a list of two values \"first\" and \"second\".\n\n"
      for((example, idx) <- transformExamples.zipWithIndex) {
        sb ++= s"#### Example ${idx + 1}\n\n"
        sb ++= example.markdownFormatted
        sb ++= "\n\n"
      }
    }

    sb.toString
  }

  private def getConstructor[T](pluginClass: Class[T]): Constructor[T] = {
    pluginClass.getConstructors.toList match {
      case constructor :: _ => constructor.asInstanceOf[Constructor[T]]
      case Nil => throw new InvalidPluginException("Plugin " + pluginClass.getName + " does not provide a constructor")
    }
  }

  private def getParameters[T](pluginClass: Class[T]): Array[ClassPluginParameter] = {
    val constructor = getConstructor(pluginClass)
    val paramAnnotations = constructor.getParameterAnnotations.map(_.collect{ case p: Param => p })
    val parameterNames = constructor.getParameters.map(_.getName)
    val parameterTypes = constructor.getGenericParameterTypes
    val defaultValues = getDefaultValues(pluginClass, parameterNames.length)

    for ((((parName, parType), defaultValue), annotations) <- parameterNames zip parameterTypes zip defaultValues zip paramAnnotations) yield {
      val pluginParam = annotations.headOption

      val label = pluginParam match {
        case Some(p) if p.label().nonEmpty => p.label()
        case _ => parName.toSentenceCase
      }

      val (description, exampleValue) = pluginParam map { pluginParam =>
        val ex = pluginParam.example()
        (pluginParam.value(), if (ex != "") Some(ex) else defaultValue)
      } getOrElse ("No description", defaultValue)

      val advanced = pluginParam exists (_.advanced())
      val visible = pluginParam forall (_.visibleInDialog())
      val dataType = ParameterType.forType(parType)
      val autoCompletion: Option[ParameterAutoCompletion] = pluginParam.flatMap(param => parameterAutoCompletion(parType, param))

      ClassPluginParameter(parName, dataType, label, description, defaultValue, exampleValue, advanced, visible, autoCompletion)
    }
  }

  private def parameterAutoCompletion[T](dataType: Type,
                                         pluginParam: Param): Option[ParameterAutoCompletion] = {
    dataType match {
      case _ if pluginParam.autoCompletionProvider() != classOf[NopPluginParameterAutoCompletionProvider] =>
        Some(explicitParameterAutoCompletionProvider(pluginParam))
      case enumClass: Class[_] if enumClass.isEnum =>
        Some(enumParameterAutoCompletion(enumClass))
      case _ =>
          None
    }
  }

  private def enumParameterAutoCompletion[T](enumClass: Class[_]): ParameterAutoCompletion = {
    val withLabel = classOf[EnumerationParameterType].isAssignableFrom(enumClass)
    ParameterAutoCompletion(
      autoCompletionProvider = EnumPluginParameterAutoCompletionProvider(enumClass),
      allowOnlyAutoCompletedValues = true,
      autoCompleteValueWithLabels = withLabel
    )
  }

  private def explicitParameterAutoCompletionProvider(pluginParam: Param): ParameterAutoCompletion = {
    assert(pluginParam.autoCompletionProvider() != classOf[NopPluginParameterAutoCompletionProvider])
    val autoCompletionProvider = PluginParameterAutoCompletionProvider.get(pluginParam.autoCompletionProvider())
    val allowOnlyAutoCompletedValues = pluginParam.allowOnlyAutoCompletedValues()
    val autoCompleteValueWithLabels = pluginParam.autoCompleteValueWithLabels()
    val autoCompletionDependsOnParameters = pluginParam.autoCompletionDependsOnParameters()
    ParameterAutoCompletion(
      autoCompletionProvider = autoCompletionProvider,
      allowOnlyAutoCompletedValues = allowOnlyAutoCompletedValues,
      autoCompleteValueWithLabels = autoCompleteValueWithLabels,
      autoCompletionDependsOnParameters = autoCompletionDependsOnParameters
    )
  }

  case class EnumPluginParameterAutoCompletionProvider(enumClass: Class[_]) extends PluginParameterAutoCompletionProvider {
    assert(enumClass.isEnum, "Trying to create enum plugin parameter auto completion provider with non-enum class: " + enumClass.getCanonicalName)
    lazy val enumValues: Seq[AutoCompletionResult] = {
      val method = enumClass.getDeclaredMethod("values")
      val enumArray = method.invoke(null).asInstanceOf[Array[Enum[_]]]
      enumArray.map {
        case enumerationParameter: EnumerationParameterType => AutoCompletionResult(enumerationParameter.id, Some(enumerationParameter.displayName))
        case enumValue: Enum[_] => AutoCompletionResult(enumValue.name(), None)
      }
    }
    override def autoComplete(searchQuery: String,
                                        projectId: String,
                                        dependOnParameterValues: Seq[String],
                                        workspace: WorkspaceReadTrait)
                                       (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
      filterResults(searchQuery, enumValues)
    }

    override def valueToLabel(projectId: String,
                              value: String,
                              dependOnParameterValues: Seq[String],
                              workspace: WorkspaceReadTrait)
                             (implicit userContext: UserContext): Option[String] = {
      enumValues.find(_.value == value).flatMap(_.label)
    }
  }

  private def getDefaultValues[T](pluginClass: Class[T], count: Int): Array[Option[AnyRef]] = {
    try {
      val clazz = Class.forName(pluginClass.getName + "$", true, pluginClass.getClassLoader)
      val module = clazz.getField("MODULE$").get(null)
      val methods = clazz.getMethods.map(method => (method.getName, method)).toMap

      for (i <- Array.range(1, count + 1)) yield {
        methods.get("$lessinit$greater$default$" + i).map(_.invoke(module))
      }
    }
    catch {
      case _: ClassNotFoundException => Array.fill(count)(None)
    }
  }
}
