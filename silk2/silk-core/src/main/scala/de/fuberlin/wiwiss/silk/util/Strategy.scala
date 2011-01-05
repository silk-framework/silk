package de.fuberlin.wiwiss.silk.util

import de.fuberlin.wiwiss.silk.config.ValidationException
import de.fuberlin.wiwiss.silk.util.StringUtils._
import com.thoughtworks.paranamer.BytecodeReadingParanamer

/**
 * A strategy which can have different implementations.
 */
trait Strategy
{
    val params : Map[String, String]

    protected def readOptionalParam(name : String) =
    {
        params.get(name)
    }

    protected def readRequiredParam(name : String) =
    {
        readOptionalParam(name) match
        {
            case Some(value) => value
            case None => throw new ValidationException("Parameter " + name + " is required")
        }
    }

    protected def readOptionalIntParam(name : String) =
    {
        readOptionalParam(name) match
        {
            case Some(IntLiteral(value)) => Some(value)
            case Some(_) => throw new ValidationException("Parameter " + name + " must be an integer")
            case None => None
        }
    }

    protected def readRequiredIntParam(name : String) =
    {
        readRequiredParam(name) match
        {
            case IntLiteral(value) => value
            case _ => throw new ValidationException("Parameter " + name + " must be an integer")
        }
    }

    protected def readOptionalDoubleParam(name : String) =
    {
        readOptionalParam(name) match
        {
            case Some(DoubleLiteral(value)) => Some(value)
            case Some(_) => throw new ValidationException("Parameter " + name + " must be a number")
            case None => None
        }
    }

    protected def readRequiredDoubleParam(name : String) =
    {
        readRequiredParam(name) match
        {
            case DoubleLiteral(value) => value
            case _ => throw new ValidationException("Parameter " + name + " must be a number")
        }
    }
}

object Strategy
{
  def getParameters[T <: Strategy](strategy : Class[T]) =
  {
    println(strategy.getConstructors.size)
    val constructor = strategy.getConstructors.head

    val paranamer = new BytecodeReadingParanamer()
    val parameterNames = paranamer.lookupParameterNames(constructor)
    val parameterTypes = constructor.getGenericParameterTypes

    parameterNames zip parameterTypes
  }

  def create[T <: Strategy](strategy : Class[T], parameters : Map[String, String])
  {
    val constructor = strategy.getConstructors.head
    val parsedParameters = parseParameters(strategy, parameters)

    constructor.newInstance(parsedParameters)
  }

  private def parseParameters[T <: Strategy](strategy : Class[T], parameterValues : Map[String, String]) : Array[AnyRef] =
  {
    for((parName, parType) <- getParameters(strategy)) yield
    {
      val value = parameterValues.get(parName) match
      {
        case Some(v) => v
        case None => ""
      }

      parType.asInstanceOf[Class[_]].getName match
      {
        case "java.lang.String" => value
        case "int" => Int.box(value.toInt)
        case "double" => Double.box(value.toDouble)
        case "boolean" => Boolean.box(value.toBoolean)
        case _ => throw new IllegalArgumentException("Unsupported parameter type: " + parType)
      }
    }
  }
}
