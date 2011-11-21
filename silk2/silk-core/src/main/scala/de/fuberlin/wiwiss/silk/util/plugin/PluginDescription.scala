/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.util.plugin

import com.thoughtworks.paranamer.BytecodeReadingParanamer
import de.fuberlin.wiwiss.silk.util.ValidationException
import java.lang.reflect.{InvocationTargetException, Constructor}

/**
 * Describes a plugin.
 */
class PluginDescription[+T <: AnyPlugin](val id: String, val label: String, val description: String, val parameters: Seq[Parameter], constructor: Constructor[T]) {
  /**
   * Creates a new instance of this plugin.
   */
  def apply(parameterValues: Map[String, String]): T = {
    val parsedParameters = parseParameters(parameterValues)

    try {
      val obj = constructor.newInstance(parsedParameters: _*)

      obj.id = id
      obj.parameters = parameterValues

      obj
    } catch {
      case ex: InvocationTargetException => throw ex.getCause
    }
  }

  private def parseParameters(parameterValues: Map[String, String]): Seq[AnyRef] = {
    for (parameter <- parameters) yield {
      parameterValues.get(parameter.name) match {
        case Some(v) => {
          try {
            parameter.dataType match {
              case Parameter.Type.String => v
              case Parameter.Type.Char => Char.box(v(0))
              case Parameter.Type.Int => Int.box(v.toInt)
              case Parameter.Type.Double => Double.box(v.toDouble)
              case Parameter.Type.Boolean => Boolean.box(v.toBoolean)
            }
          }
          catch {
            case ex: NumberFormatException => throw new ValidationException(label + " has an invalid value for parameter " + parameter.name + ". Value must be of type " + parameter.dataType, ex)
          }
        }
        case None if parameter.defaultValue.isDefined => {
          parameter.defaultValue.get
        }
        case None => {
          throw new ValidationException("Parameter '" + parameter.name + "' is required for " + label)
        }
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
  def apply[T <: AnyPlugin](pluginClass: Class[T]): PluginDescription[T] = {
    getAnnotation(pluginClass) match {
      case Some(annotation) => createFromAnnotation(pluginClass, annotation)
      case None => createFromClass(pluginClass)
    }
  }

  private def getAnnotation[T <: AnyPlugin](pluginClass: Class[T]): Option[Plugin] = {
    Option(pluginClass.getAnnotation(classOf[Plugin]))
  }

  private def createFromAnnotation[T <: AnyPlugin](pluginClass: Class[T], annotation: Plugin) = {
    new PluginDescription(
      id = annotation.id,
      label = annotation.label,
      description = annotation.description,
      parameters = getParameters(pluginClass),
      constructor = getConstructor(pluginClass)
    )
  }

  private def createFromClass[T <: AnyPlugin](pluginClass: Class[T]) = {
    new PluginDescription(
      id = pluginClass.getSimpleName,
      label =  pluginClass.getSimpleName,
      description = "",
      parameters = getParameters(pluginClass),
      constructor = getConstructor(pluginClass)
    )
  }

  private def getConstructor[T <: AnyPlugin](pluginClass: Class[T]): Constructor[T] = {
    pluginClass.getConstructors.toList match {
      case constructor :: _ => constructor.asInstanceOf[Constructor[T]]
      case Nil => throw new InvalidPluginException("Plugin " + pluginClass.getName + " does not provide a constructor")
    }
  }

  private def getParameters[T <: AnyPlugin](pluginClass: Class[T]): Array[Parameter] = {
    val constructor = getConstructor(pluginClass)

    val paranamer = new BytecodeReadingParanamer()
    val parameterNames = paranamer.lookupParameterNames(constructor)
    val parameterTypes = constructor.getGenericParameterTypes
    val defaultValues = getDefaultValues(pluginClass, parameterNames.size)

    for (((parName, parType), defaultValue) <- parameterNames zip parameterTypes zip defaultValues) yield {
      if (!parType.isInstanceOf[Class[_]]) throw new InvalidPluginException("Unsupported parameter type in plugin " + pluginClass.getName + ": " + parType)

      val dataType = parType.asInstanceOf[Class[_]].getName match {
        case "java.lang.String" => Parameter.Type.String
        case "char" => Parameter.Type.Char
        case "int" => Parameter.Type.Int
        case "double" => Parameter.Type.Double
        case "boolean" => Parameter.Type.Boolean
        case _ => throw new InvalidPluginException("Unsupported parameter type: " + parType)
      }

      Parameter(parName, dataType, "No description", defaultValue)
    }
  }

  private def getDefaultValues[T <: AnyPlugin](pluginClass: Class[T], count: Int): Array[Option[AnyRef]] = {
    try {
      val clazz = Class.forName(pluginClass.getName + "$")
      val module = clazz.getField("MODULE$").get(null)
      val methods = clazz.getMethods.map(method => (method.getName, method)).toMap

      for (i <- Array.range(1, count + 1)) yield {
        methods.get("init$default$" + i).map(_.invoke(module))
      }
    }
    catch {
      case ex: ClassNotFoundException => Array.fill(count)(None)
    }
  }
}