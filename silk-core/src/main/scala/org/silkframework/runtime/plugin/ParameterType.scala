package org.silkframework.runtime.plugin

import java.lang.reflect.{ParameterizedType, Type}
import java.net.{URLDecoder, URLEncoder}
import java.util.logging.Logger

import org.silkframework.config.{DefaultConfig, Prefixes, ProjectReference, TaskReference}
import org.silkframework.dataset.rdf.SparqlEndpointDatasetParameter
import org.silkframework.runtime.resource.{EmptyResourceManager, Resource, ResourceManager, WritableResource}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{AesCrypto, Identifier, Uri}

import scala.language.existentials
import scala.reflect.ClassTag
import scala.util.Try

/**
  * Represents a plugin parameter type and provides serialization.
  *
  * @tparam T The underlying type of this datatype, e.g., Int
  */
sealed abstract class ParameterType[T: ClassTag] {

  /**
    * The underlying type.
    */
  private val dataType = implicitly[ClassTag[T]].runtimeClass

  /**
    * User-readable name of this type.
    */
  def name: String

  /**
    * User-readable description of this type to be displayed.
    */
  def description: String = ""

  def hasType(givenType: Type): Boolean = {
    givenType match {
      case pt: ParameterizedType => pt.getRawType.toString == dataType.toString
      case t: Type => t.toString == dataType.toString
    }
  }

  /**
    * Parses a value from its string representation.
    *
    * @param str            The string representation.
    * @param prefixes       The current prefixes for resolving prefixed names
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
  def toString(value: T)(implicit prefixes: Prefixes): String = Option(value) map (_.toString) getOrElse ""

  /**
    * Short name of this type.
    */
  override def toString: String = dataType.getSimpleName

}

/**
  * Provides all available parameter types.
  */
object ParameterType {
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  /**
    * All available static parameter types.
    */
  private val allStaticTypes: Seq[ParameterType[_]] = {
    Seq(StringType, CharType, IntType, DoubleType, BooleanType, IntOptionType, StringMapType, UriType, ResourceType,
      WritableResourceType, ProjectReferenceType, TaskReferenceType, MultilineStringParameterType, SparqlEndpointDatasetParameterType, LongType,
      PasswordParameterType)
  }

  /**
    * Retrieves the parameter type for a specific underlying type.
    *
    * @throws InvalidPluginException If no parameter type is available for the given class.
    */
  def forType(dataType: Type): ParameterType[_] = {
    dataType match {
      case enumClass: Class[_] if enumClass.isEnum =>
        EnumerationType(enumClass)
      case _ =>
        allStaticTypes.find(_.hasType(dataType))
            .getOrElse(throw new InvalidPluginException("Unsupported parameter type: " + dataType))
    }
  }

  object StringType extends ParameterType[String] {

    override def name: String = "string"

    override def description: String = "A character string."

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): String = {
      str
    }

  }

  object CharType extends ParameterType[Char] {

    override def name: String = "char"

    override def description: String = "A single character."

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Char = {
      if (str.length == 1) {
        str(0)
      }
      else {
        throw new ValidationException("Value must be a single character.")
      }
    }

  }

  object IntType extends ParameterType[Int] {

    override def name: String = "int"

    override def description: String = "An integer number."

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Int = {
      str.toInt
    }

  }

  object LongType extends ParameterType[Long] {

    override def name: String = "Long"

    override def description: String = "A Long number."

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Long = {
      str.toLong
    }

  }

  object DoubleType extends ParameterType[Double] {

    override def name: String = "double"

    override def description: String = "A floating-point number."

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Double = {
      str.toDouble
    }

  }

  object BooleanType extends ParameterType[Boolean] {

    override def name: String = "boolean"

    override def description: String = "Either true or false."

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Boolean = {
      str.toLowerCase match {
        case "true" | "1" => true
        case "false" | "0" => false
        case _ => throw new ValidationException("Value must be either 'true' or 'false'")
      }
    }

  }

  object IntOptionType extends ParameterType[Option[Int]] {

    override def name: String = "option[int]"

    override def description: String = "An optional integer number."

    override def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Option[Int] = {
      if(str.trim.isEmpty) {
        None
      } else {
        Some(str.toInt)
      }
    }

    override def toString(value: Option[Int])(implicit prefixes: Prefixes): String = {
      value.map(_.toString).getOrElse("")
    }

  }

  object StringMapType extends ParameterType[Map[String, String]] {

    override def name: String = "stringmap"

    override def description: String = "A map of the form 'Key1:Value1,Key2:Value2'"

    private val utf8: String = "UTF8"

    def fromString(str: String)(implicit prefixes: Prefixes = Prefixes.empty, resourceLoader: ResourceManager = EmptyResourceManager): Map[String, String] = {
      str.split(',').map(_.split(':')).map(v => Tuple2(URLDecoder.decode(v(0), utf8), URLDecoder.decode(v(1), utf8))).toMap
    }

    override def toString(value: Map[String, String])(implicit prefixes: Prefixes): String = {
      val strValues = for ((k, v) <- value) yield URLEncoder.encode(k, utf8) + ":" + URLEncoder.encode(v, utf8)
      strValues.mkString(",")
    }

  }

  object UriType extends ParameterType[Uri] {

    override def name: String = "uri"

    override def description: String = "Either a full URI or a prefixed name."

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Uri = {
      Uri.parse(str, prefixes)
    }

    override def toString(value: Uri)(implicit prefixes: Prefixes): String = {
      value.serialize(prefixes)
    }
  }

  object ResourceType extends ParameterType[Resource] {

    override def name: String = "resource"

    override def description: String = "Either the name of a project resource or a full URI."

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Resource = {
      if (str.trim.isEmpty) {
        throw new ValidationException("Resource cannot be empty")
      } else {
        resourceLoader.get(str)
      }
    }

  }

  object WritableResourceType extends ParameterType[WritableResource] {

    override def name: String = "resource"

    override def description: String = "Either the name of a project resource or a full URI."

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): WritableResource = {
      if (str.trim.isEmpty) {
        throw new ValidationException("Resource cannot be empty")
      } else {
        resourceLoader.get(str)
      }
    }

  }

  object ProjectReferenceType extends ParameterType[ProjectReference] {

    override def name: String = "project"

    override def description: String = "The identifier of a project in the workspace."

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): ProjectReference = {
      ProjectReference(Identifier(str))
    }

    override def toString(value: ProjectReference)(implicit prefixes: Prefixes): String = {
      value.id
    }

  }

  object TaskReferenceType extends ParameterType[TaskReference] {

    override def name: String = "task"

    override def description: String = "The identifier of a task in the same project."

    def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): TaskReference = {
      TaskReference(Identifier(str))
    }

    override def toString(value: TaskReference)(implicit prefixes: Prefixes): String = {
      value.id
    }

  }

  case class EnumerationType(enumType: Class[_]) extends ParameterType[Enum[_]] {
    require(enumType.isEnum)

    private val enumConstants = enumType.asInstanceOf[Class[Enum[_]]].getEnumConstants

    private val valueList = enumerationValues.mkString(", ")

    override def name: String = "enumeration"

    override def description: String = "One of the following values: " + valueList

    override def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): Enum[_] = {
      enumConstants.find {
        case e: EnumerationParameterType =>
          e.id == str.trim || e.name == str.trim
        case c: Enum[_] =>
          c.name == str.trim
      } getOrElse (throw new ValidationException(s"Invalid enumeration value '$str'. Allowed values are: $valueList"))
    }

    def enumerationValues: Seq[String] = enumConstants map enumerationValue

    /** The display names. The Enum has to implement [[EnumerationParameterType]], else the enum name is used. */
    def displayNames: Seq[String] = enumConstants map {
      case e: EnumerationParameterType =>
        e.displayName
      case c: Enum[_] =>
        c.name()
    }

    private def enumerationValue(value: Enum[_]): String = {
      value match {
        case e: EnumerationParameterType =>
          e.id
        case c: Enum[_] =>
          c.name()
      }
    }

    override def toString(value: Enum[_])(implicit prefixes: Prefixes): String = enumerationValue(value)
  }

  object MultilineStringParameterType extends ParameterType[MultilineStringParameter] {
    override def name: String = "multiline string"

    override def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): MultilineStringParameter = MultilineStringParameter(str)
  }

  object PasswordParameterType extends ParameterType[PasswordParameter] {
    // This preamble should be added to all serializations to mark the string as a encrypted password, else it will be interpreted as plain
    final val PREAMBLE = "PASSWORD_PARAMETER:"
    override def name: String = "password"

    override def description: String = "A password string."

    lazy val key: String = {
      Try(DefaultConfig.instance().getString("plugin.parameters.password.crypt.key")).getOrElse {
        log.warning("No valid value set for plugin.parameters.password.crypt.key, using insecure default key!")
        "1234567890123456"
      }
    }

    override def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): PasswordParameter = {
      val encryptedPassword = if(str.startsWith(PREAMBLE)) {
        str.stripPrefix(PREAMBLE)
      } else {
        AesCrypto.encrypt(key, str)
      }
      PasswordParameter(encryptedPassword)
    }
  }

  object SparqlEndpointDatasetParameterType extends ParameterType[SparqlEndpointDatasetParameter] {
    override def name: String = "SPARQL endpoint"

    override def fromString(str: String)(implicit prefixes: Prefixes, resourceLoader: ResourceManager): SparqlEndpointDatasetParameter = {
      SparqlEndpointDatasetParameter(str)
    }
  }
}
