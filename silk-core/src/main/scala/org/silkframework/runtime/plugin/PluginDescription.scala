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

import java.lang.reflect.{Constructor, InvocationTargetException, ParameterizedType}

import com.thoughtworks.paranamer.BytecodeReadingParanamer
import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import scala.language.existentials
import scala.util.control.NonFatal

/**
  * Describes a plugin.
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
class PluginDescription[+T](val id: Identifier, val categories: Set[String], val label: String, val description: String,
                            val documentation: String, val parameters: Seq[Parameter], constructor: Constructor[T]) {

  /**
    * The plugin class.
    */
  def pluginClass: Class[_ <: T] = {
    constructor.getDeclaringClass
  }

  /**
   * Creates a new instance of this plugin.
   */
  def apply(parameterValues: Map[String, String] = Map.empty)(implicit prefixes: Prefixes, resources: ResourceManager = EmptyResourceManager()): T = {
    val parsedParameters = parseParameters(parameterValues)
    try {
      constructor.newInstance(parsedParameters: _*)
    } catch {
      case ex: InvocationTargetException => throw ex.getCause
    }
  }

  /**
   * Retrieves the parameters values of a given plugin instance.
   */
  def parameterValues(plugin: AnyRef)(implicit prefixes: Prefixes): Map[String, String] = {
    parameters.map(param => (param.name, param.stringValue(plugin))).toMap
  }

  override def toString = label

  private def parseParameters(parameterValues: Map[String, String])(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Seq[AnyRef] = {
    for (parameter <- parameters) yield {
      parameterValues.get(parameter.name) match {
        case Some(v) =>
          try {
            parameter.dataType.fromString(v).asInstanceOf[AnyRef]
          } catch {
            case NonFatal(ex) => throw new ValidationException(label + " has an invalid value for parameter " + parameter.name + ". Value must be a valid " + parameter.dataType + ". Issue: " + ex.getMessage, ex)
          }
        case None if parameter.defaultValue.isDefined =>
          parameter.defaultValue.get
        case None =>
          throw new ValidationException("Parameter '" + parameter.name + "' is required for " + label)
      }
    }
  }

}

/**
 * Factory for plugin description.
 */
object PluginDescription {
  /**
   * Creates a new plugin description from a class.
   */
  def apply[T](pluginClass: Class[T]): PluginDescription[T] = {
    getAnnotation(pluginClass) match {
      case Some(annotation) => createFromAnnotation(pluginClass, annotation)
      case None => createFromClass(pluginClass)
    }
  }

  private def getAnnotation[T](pluginClass: Class[T]): Option[Plugin] = {
    Option(pluginClass.getAnnotation(classOf[Plugin]))
  }

  private def createFromAnnotation[T](pluginClass: Class[T], annotation: Plugin) = {
    new PluginDescription(
      id = annotation.id,
      label = annotation.label,
      categories = annotation.categories.toSet,
      description = annotation.description.stripMargin,
      documentation = addTransformDocumentation(annotation.documentation, pluginClass),
      parameters = getParameters(pluginClass),
      constructor = getConstructor(pluginClass)
    )
  }

  private def createFromClass[T](pluginClass: Class[T]) = {
    new PluginDescription(
      id = Identifier.fromAllowed(pluginClass.getSimpleName),
      label = pluginClass.getSimpleName,
      categories = Set("Uncategorized"),
      description = "",
      documentation = addTransformDocumentation("", pluginClass),
      parameters = getParameters(pluginClass),
      constructor = getConstructor(pluginClass)
    )
  }

  private def addTransformDocumentation(documentation: String, pluginClass: Class[_]) = {
    val sb = new StringBuilder(documentation)

    val transformExamples = TransformExampleValue.retrieve(pluginClass)
    if(transformExamples.nonEmpty) {
      sb ++= "**Example Values**"
      sb ++= "\n\n"
      for(example <- transformExamples) {
        sb ++= example.formatted
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

  private def getParameters[T](pluginClass: Class[T]): Array[Parameter] = {
    val constructor = getConstructor(pluginClass)
    val paramAnnotations = constructor.getParameterAnnotations.map(_.collect{ case p: Param => p })
    val paranamer = new BytecodeReadingParanamer()
    val parameterNames = paranamer.lookupParameterNames(constructor)
    val parameterTypes = constructor.getGenericParameterTypes
    val defaultValues = getDefaultValues(pluginClass, parameterNames.size)

    for ((((parName, parType), defaultValue), annotations) <- parameterNames zip parameterTypes zip defaultValues zip paramAnnotations) yield {
      val pluginParam = annotations.headOption

      val label = pluginParam match {
        case Some(p) if p.label().nonEmpty => p.label()
        case _ => parName.flatMap(c => if(c.isUpper) " " + c.toLower else c.toString)
      }

      val (description, exampleValue) = pluginParam map { pluginParam =>
        val ex = pluginParam.example()
        (pluginParam.value(), if (ex != "") Some(ex) else defaultValue)
      } getOrElse ("No description", defaultValue)

      val advanced = pluginParam exists (_.advanced())
      val visible = pluginParam forall (_.visibleInDialog())

      val dataType = ParameterType.forType(parType)
      Parameter(parName, dataType, label, description, defaultValue, exampleValue, advanced, visible)
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
      case ex: ClassNotFoundException => Array.fill(count)(None)
    }
  }
}