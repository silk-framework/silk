package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.{WritableResource, Resource, EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import scala.reflect.ClassTag

/**
  * Represents a plugin parameter type and provides serialization.
  *
  * @tparam T The underlying type of this datatype, e.g., Int
  */
sealed abstract class ParameterType[T : ClassTag] {

  /**
    * The underlying type.
    */
  def dataType = implicitly[ClassTag[T]].runtimeClass

  /**
    * Parses a value from its string representation.
    *
    * @param str The string representation.
    * @param prefixes The current prefixes for resolving prefixed names
    * @param resourceLoader The current resources for resolving resource references.
    * @return Either returns the parsed value or throws an exception.
    */
  def fromString(str: String)(implicit prefixes: Prefixes = Prefixes.empty, resourceLoader: ResourceManager = EmptyResourceManager): T

  /**
    * Serializes a value to its string representation.
    * By default just calls toString on the value.
    *
    * @param value The value to be serialized.
    * @return The string representation of the value that can be parsed by calling fromString on the same datatype.
    */
  def toString(value: T): String = value.toString

  /**
    * Short name of this type.
    */
  override def toString = dataType.getSimpleName

}

/**
  * Provides all available parameter types.
  */
object ParameterType {

  /**
    * All available paramter types.
    */
  val all: Seq[ParameterType[_]] = {
    Seq(StringType, CharType, IntType, DoubleType, BooleanType, UriType, ResourceType, WritableResourceType)
  }

  /**
    * Retrieves the parameter type for a specific underlying class.
    *
    * @throws InvalidPluginException If no parameter type is available for the given class.
    */
  def forClass(dataClass: Class[_]): ParameterType[_] = {
    all.find(_.dataType.getName == dataClass.getName)
       .getOrElse(throw new InvalidPluginException("Unsupported parameter type: " + dataClass))
  }

  object StringType extends ParameterType[String] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): String = {
      str
    }

  }

  object CharType extends ParameterType[Char] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Char = {
      if(str.length == 1) str(0)
      else throw new ValidationException("Value must be a single character.")
    }

  }

  object IntType extends ParameterType[Int] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Int = {
      str.toInt
    }

  }

  object DoubleType extends ParameterType[Double] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Double = {
      str.toDouble
    }

  }

  object BooleanType extends ParameterType[Boolean] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Boolean = {
      str.toLowerCase match {
        case "true" | "1" => true
        case "false" | "0" => false
        case _ => throw new ValidationException("Value must be either 'true' or 'false'")
      }
    }

  }

  object UriType extends ParameterType[Uri] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Uri = {
      Uri.parse(str, prefixes)
    }
  }

  object ResourceType extends ParameterType[Resource] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Resource = {
      resourceLoader.get(str, mustExist = false)
    }

  }

  object WritableResourceType extends ParameterType[WritableResource] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): WritableResource = {
      resourceLoader.get(str, mustExist = false)
    }

  }

}
