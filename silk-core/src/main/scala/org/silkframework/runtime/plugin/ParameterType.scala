package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.{WritableResource, Resource, EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.serialization.ValidationException

/**
  * Represents a plugin parameter type and provides serialization.
  *
  * @tparam T The underlying type of this datatype, e.g., scala.Int
  */
sealed trait ParameterType[T] {

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

}

/**
  * Provides all available parameter types.
  */
object ParameterType {

  val all = Seq(StringType, CharType, IntType, DoubleType, BooleanType, ResourceType, WritableResourceType)

  object StringType extends ParameterType[String] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): String = {
      str
    }

  }

  object CharType extends ParameterType[Char] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Char = {
      if(str.length == 1) Char.box(str(0))
      else throw new ValidationException("Value must be a single character.")
    }

  }

  object IntType extends ParameterType[Int] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Int = {
      Int.box(str.toInt)
    }

  }

  object DoubleType extends ParameterType[Double] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Double = {
      Double.box(str.toDouble)
    }

  }

  object BooleanType extends ParameterType[Boolean] {

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Boolean = {
      str.toLowerCase match {
        case "true" | "1" => Boolean.box(true)
        case "false" | "0" => Boolean.box(false)
        case _ => throw new ValidationException("Value must be either 'true' or 'false'")
      }
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
