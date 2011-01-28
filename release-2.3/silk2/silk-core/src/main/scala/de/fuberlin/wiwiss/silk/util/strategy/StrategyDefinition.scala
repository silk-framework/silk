package de.fuberlin.wiwiss.silk.util.strategy

import java.lang.reflect.Constructor
import com.thoughtworks.paranamer.BytecodeReadingParanamer

class StrategyDefinition[+T <: Strategy](val id : String, val label : String, val description : String, val parameters : Seq[Parameter],
                                         constructor : Constructor[T])
{
  def apply(parameterValues : Map[String, String]) : T =
  {
    val parsedParameters = parseParameters(parameterValues)

    constructor.newInstance(parsedParameters : _*)
  }

  private def parseParameters(parameterValues : Map[String, String]) : Seq[AnyRef] =
  {
    for(parameter <- parameters) yield
    {
      parameterValues.get(parameter.name) match
      {
        case Some(v) =>
        {
          parameter.dataType match
          {
            case Parameter.Type.String => v
            case Parameter.Type.Char => Char.box(v(0))
            case Parameter.Type.Int => Int.box(v.toInt)
            case Parameter.Type.Double => Double.box(v.toDouble)
            case Parameter.Type.Boolean => Boolean.box(v.toBoolean)
          }
        }
        case None if parameter.defaultValue.isDefined =>
        {
          parameter.defaultValue.get
        }
        case None =>
        {
          throw new IllegalArgumentException("Parameter '" + parameter.name + "' is required")
        }
      }
    }
  }

}

object StrategyDefinition
{
  def apply[T <: Strategy](strategy : Class[T]) : StrategyDefinition[T] =
  {
    val annotation = getAnnotation(strategy)
    val parameters = getParameters(strategy)
    val constructor = getConstructor(strategy)

    new StrategyDefinition(annotation.id, annotation.label, annotation.description, parameters, constructor)
  }

  private def getAnnotation[T <: Strategy](strategy : Class[T]) : StrategyAnnotation =
  {
    val annotation = strategy.getAnnotation(classOf[StrategyAnnotation])
    if(annotation == null) throw new InvalidStrategyException("Strategy " + strategy.getName + " must be annotated with StrategyAnnotation.")
    annotation
  }

  private def getConstructor[T <: Strategy](strategy : Class[T]) : Constructor[T] =
  {
    strategy.getConstructors.toList match
    {
      case constructor :: _ => constructor.asInstanceOf[Constructor[T]]
      case Nil => throw new InvalidStrategyException("Strategy " + strategy.getName + " does not provide a constructor")
    }
  }

  private def getParameters[T <: Strategy](strategy : Class[T]) : Array[Parameter] =
  {
    val constructor = getConstructor(strategy)

    val paranamer = new BytecodeReadingParanamer()
    val parameterNames = paranamer.lookupParameterNames(constructor)
    val parameterTypes = constructor.getGenericParameterTypes
    val defaultValues = getDefaultValues(strategy, parameterNames.size)

    for(((parName, parType), defaultValue) <- parameterNames zip parameterTypes zip defaultValues) yield
    {
      if(!parType.isInstanceOf[Class[_]]) throw new InvalidStrategyException("Unsupported parameter type in strategy " + strategy.getName + ": " + parType)

      val dataType = parType.asInstanceOf[Class[_]].getName match
      {
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

  private def getDefaultValues[T <: Strategy](strategy : Class[T], count : Int) : Array[Option[AnyRef]] =
  {
    try
    {
      val clazz = Class.forName(strategy.getName + "$")
      val module = clazz.getField("MODULE$").get(null)
      val methods = clazz.getMethods.map(method => (method.getName, method)).toMap

      for(i <- Array.range(1, count + 1)) yield
      {
        methods.get("init$default$" + i).map(_.invoke(module))
      }
    }
    catch
    {
      case ex : ClassNotFoundException => Array.fill(count)(None)
    }
  }
}