package de.fuberlin.wiwiss.silk.util.strategy

import java.lang.reflect.Constructor
import com.thoughtworks.paranamer.BytecodeReadingParanamer
import de.fuberlin.wiwiss.silk.util.ValidationException
import java.lang.annotation.Annotation

/**
 * Describes a strategy.
 */
class StrategyDescription[+T <: Strategy](val id: String, val label: String, val description: String, val parameters: Seq[Parameter], constructor: Constructor[T]) {
  /**
   * Creates a new instance of this strategy.
   */
  def apply(parameterValues: Map[String, String]): T = {
    val parsedParameters = parseParameters(parameterValues)

    val obj = constructor.newInstance(parsedParameters: _*)

    obj.id = id
    obj.parameters = parameterValues

    obj
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
 * Factory for strategy description.
 */
object StrategyDescription {
  /**
   * Creates a new strategy description from a class.
   */
  def apply[T <: Strategy](strategy: Class[T]): StrategyDescription[T] = {
    getAnnotation(strategy) match {
      case Some(annotation) => createFromAnnotation(strategy, annotation)
      case None => createFromClass(strategy)
    }
  }

  private def getAnnotation[T <: Strategy](strategy: Class[T]): Option[StrategyAnnotation] = {
    Option(strategy.getAnnotation(classOf[StrategyAnnotation]))
  }

  private def createFromAnnotation[T <: Strategy](strategy: Class[T], annotation: StrategyAnnotation) = {
    new StrategyDescription(
      id = annotation.id,
      label = annotation.label,
      description = annotation.description,
      parameters = getParameters(strategy),
      constructor = getConstructor(strategy)
    )
  }

  private def createFromClass[T <: Strategy](strategy: Class[T]) = {
    new StrategyDescription(
      id = strategy.getSimpleName,
      label =  strategy.getSimpleName,
      description = "",
      parameters = getParameters(strategy),
      constructor = getConstructor(strategy)
    )
  }

  private def getConstructor[T <: Strategy](strategy: Class[T]): Constructor[T] = {
    strategy.getConstructors.toList match {
      case constructor :: _ => constructor.asInstanceOf[Constructor[T]]
      case Nil => throw new InvalidStrategyException("Strategy " + strategy.getName + " does not provide a constructor")
    }
  }

  private def getParameters[T <: Strategy](strategy: Class[T]): Array[Parameter] = {
    val constructor = getConstructor(strategy)

    val paranamer = new BytecodeReadingParanamer()
    val parameterNames = paranamer.lookupParameterNames(constructor)
    val parameterTypes = constructor.getGenericParameterTypes
    val defaultValues = getDefaultValues(strategy, parameterNames.size)

    for (((parName, parType), defaultValue) <- parameterNames zip parameterTypes zip defaultValues) yield {
      if (!parType.isInstanceOf[Class[_]]) throw new InvalidStrategyException("Unsupported parameter type in strategy " + strategy.getName + ": " + parType)

      val dataType = parType.asInstanceOf[Class[_]].getName match {
        case "java.lang.String" => Parameter.Type.String
        case "char" => Parameter.Type.Char
        case "int" => Parameter.Type.Int
        case "double" => Parameter.Type.Double
        case "boolean" => Parameter.Type.Boolean
        case _ => throw new InvalidStrategyException("Unsupported parameter type: " + parType)
      }

      Parameter(parName, dataType, "No description", defaultValue)
    }
  }

  private def getDefaultValues[T <: Strategy](strategy: Class[T], count: Int): Array[Option[AnyRef]] = {
    try {
      val clazz = Class.forName(strategy.getName + "$")
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