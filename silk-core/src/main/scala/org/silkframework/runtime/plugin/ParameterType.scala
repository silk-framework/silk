package org.silkframework.runtime.plugin

import org.silkframework.config.{DefaultConfig, ProjectReference, TaskReference}
import org.silkframework.dataset.rdf.SparqlEndpointDatasetParameter
import org.silkframework.entity.Restriction
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.types._
import org.silkframework.runtime.resource.{Resource, WritableResource}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{AesCrypto, Identifier, Uri}

import java.lang.reflect.{ParameterizedType, Type}
import java.net.{URLDecoder, URLEncoder}
import java.security.spec.InvalidKeySpecException
import java.time.Duration
import java.util.logging.{Level, Logger}
import javax.crypto.SecretKey
import scala.language.existentials
import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/** Represents a plugin parameter type and provides serialization. */
sealed abstract class ParameterType[T: ClassTag] {
  /**
    * Identifying name of this type.
    */
  def name: String

  /**
    * User-readable description of this type to be displayed.
    */
  def description: String = name

  /**
    * The underlying type.
    */
  val dataType: Class[_] = implicitly[ClassTag[T]].runtimeClass

  def hasType(givenType: Type): Boolean = {
    givenType match {
      case pt: ParameterizedType => pt.getRawType.toString == dataType.toString
      case t: Type => t.toString == dataType.toString
    }
  }

  /** How values of this parameter type are represented in JSON, e.g. string or object. */
  def jsonSchemaType: String

  /**
    * Short name of this type.
    */
  override def toString: String = dataType.getSimpleName

  /** The default string representation of the value. */
  def toString(value: T)
              (implicit pluginContext: PluginContext): String
}

object ParameterType {
  /**
    * Retrieves the parameter type for a specific underlying type.
    *
    * @throws InvalidPluginException If no parameter type is available for the given class.
    */
  def forType(dataType: Type): ParameterType[_] = {
    StringParameterType.forTypeOpt(dataType) match {
      case Some(stringParameterType) => stringParameterType
      case None =>
        dataType match {
          case cl: Class[_] if classOf[PluginObjectParameter].isAssignableFrom(cl) =>
            PluginObjectParameterType(cl.asInstanceOf[Class[PluginObjectParameter]])
          case cl: Class[_] if classOf[PluginObjectParameterNoSchema].isAssignableFrom(cl) =>
            // Parameters that generate no schema definition and thus cannot be handled generically, e.g. transform rule.
            PluginObjectParameterNoSchemaType(cl.asInstanceOf[Class[PluginObjectParameterNoSchema]])
          case cl: Class[_] if classOf[PluginStringParameter].isAssignableFrom(cl) =>
            GenericPluginStringParameterType(cl)
          case _ =>
            throw new InvalidPluginException("Unsupported parameter type: " + dataType)
        }
    }
  }
}

/** Trait that marks a class as a plugin parameter.
  * There will be done additional tests on start-up for all plugins implementing this trait, e.g. if JSON and XML formats are registered etc. */
@PluginType()
trait PluginObjectParameter extends AnyPlugin {
  /** defines if the parameter type has a schema definition, i.e. is composed completely from [[ParameterType]] elements. */
  def hasSchemaDefinition: Boolean = true

  /** Defines if the parameter type is serialized. This should be true in general, except for parameters of classes that
    * are not persisted or part of the HTTP API. */
  def hasSerialization: Boolean = true
}

/** Plugin parameter type that is of type object and represents nested values. The default string representation is to JSON. */
trait PluginObjectParameterTypeTrait extends ParameterType[PluginObjectParameter] with AnyPlugin {
  def pluginObjectParameterClass: Class[_ <: AnyPlugin]

  override def jsonSchemaType: String = "object"

  def pluginDescription: Option[PluginDescription[_]] = PluginRegistry.pluginDescription(pluginObjectParameterClass)

  override def toString(value: PluginObjectParameter)(implicit pluginContext: PluginContext): String = {
    // There is no string representation
    ???
  }
}

/** Parameter type that is represented by a plugin class.
  * It is expected that this type has proper serialization formats implemented, e.g. for XML and JSON etc. */
case class PluginObjectParameterType(pluginObjectParameterClass: Class[_ <: AnyPlugin]) extends PluginObjectParameterTypeTrait {
  override def name: String = "objectParameter"
}

/** Should be used for parameters that are either too complex and cannot be edited in a generic plugin dialog or they are not
  * composed of parameters that themselves have a schema. */
trait PluginObjectParameterNoSchema extends PluginObjectParameter {
  override def hasSchemaDefinition: Boolean = false
}

/** This should ONLY be used for parameters of classes that are never persisted or part of the HTTP API. */
trait PluginObjectParameterNoSchemaAndSerialization extends PluginObjectParameterNoSchema {
  override def hasSerialization: Boolean = false
}

case class PluginObjectParameterNoSchemaType(pluginObjectParameterClass: Class[_ <: AnyPlugin]) extends PluginObjectParameterTypeTrait {
  override def name: String = "pluginObjectParameterNoSchema"
}

/** Trait for plugin parameters that can be serialized to string, but that are not defined in the core package.
  * They must have a PluginStringParameterType implementation (available in the plugin registry) that handles it. */
trait PluginStringParameter

/** Parameter type wrapper for PluginStringParameter types. This is necessary since the PluginStringParameterType plugin
  * might not be registered yet in the plugin registry at creation time. */
case class GenericPluginStringParameterType(pluginStringParameterClass: Class[_]) extends StringParameterType[Any] {
  private lazy val stringPlugin = {
    val pluginDescriptions = PluginRegistry.availablePluginsForClass(classOf[PluginStringParameterType[_]])
    implicit val context: PluginContext = PluginContext.empty
    val plugins = pluginDescriptions.map(_.apply())
    plugins.map(_.asInstanceOf[PluginStringParameterType[_]]).find(_.dataType == pluginStringParameterClass).getOrElse(
      throw new ValidationException("No parameter type available to handle string parameter type class " + pluginStringParameterClass.getName)
    )
  }

  override def fromString(str: String)(implicit context: PluginContext): Any = {
    stringPlugin.fromString(str)
  }

  override def name: String = stringPlugin.name
}

/** Parameter type that implements a PluginStringParameter parameter. */
@PluginType()
abstract class PluginStringParameterType[T <: PluginStringParameter : ClassTag] extends StringParameterType[T] with AnyPlugin

/**
  * Represents a plugin parameter type and provides string-based serialization.
  * This is suitable for plugin parameters that have a simple string based serialization.
  *
  * @tparam T The underlying type of this datatype, e.g., Int
  */
@PluginType()
abstract class StringParameterType[T: ClassTag] extends ParameterType[T] {
  /**
    * Parses a value from its string representation.
    *
    * @param str            The string representation.
    * @param prefixes       The current prefixes for resolving prefixed names
    * @param resourceLoader The current resources for resolving resource references.
    * @return Either returns the parsed value or throws an exception.
    */
  def fromString(str: String)(implicit context: PluginContext): T

  /**
    * Serializes a value to its string representation.
    * By default just calls toString on the value.
    *
    * @param value The value to be serialized.
    * @return The string representation of the value that can be parsed by calling fromString on the same datatype.
    */
  def toString(value: T)(implicit pluginContext: PluginContext): String = Option(value) map (_.toString) getOrElse ""

  override def jsonSchemaType: String = "string"
}

/**
  * Provides all available parameter types.
  */
object StringParameterType {
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  /**
    * All available static parameter types.
    */
  private val allStaticTypes: Seq[StringParameterType[_]] = {
    Seq(StringType, CharType, IntType, DoubleType, BooleanType, IntOptionType, StringMapType, UriType, ResourceType,
      WritableResourceType, ResourceOptionType, DurationType, ProjectReferenceType, TaskReferenceType, MultilineStringParameterType,
      SparqlEndpointDatasetParameterType, LongType, GraphUriParameterType, TemplateParameterType,
      PasswordParameterType, IdentifierType, IdentifierOptionType, StringIterableParameterType, RestrictionType,
      Jinja2CodeParameterType, JsonCodeParameterType, SparqlCodeParameterType, SqlCodeParameterType, XmlCodeParameterType,
      YamlCodeParameterType, PythonCodeParameterType, TurtleCodeParameterType, LocaleOptionType)
  }

  /**
    * Retrieves the parameter type for a specific underlying type.
    *
    * @throws InvalidPluginException If no parameter type is available for the given class.
    */
  def forType(dataType: Type): StringParameterType[_] = {
    forTypeOpt(dataType).getOrElse(
      throw new InvalidPluginException("Unsupported parameter type: " + dataType))
  }

  def forTypeOpt(dataType: Type): Option[StringParameterType[_]] = {
    dataType match {
      case enumClass: Class[_] if enumClass.isEnum =>
        Some(EnumerationType(enumClass))
      case optionEnumClass: ParameterizedType if
        optionEnumClass.getRawType == classOf[Option[_]] &&
          optionEnumClass.getActualTypeArguments.length == 1 &&
          optionEnumClass.getActualTypeArguments.head.isInstanceOf[Class[_]] &&
          optionEnumClass.getActualTypeArguments.head.asInstanceOf[Class[_]].isEnum =>
        val enumClass = optionEnumClass.getActualTypeArguments.head.asInstanceOf[Class[_]]
        Some(OptionEnumerationType(enumClass))
      case _ =>
        allStaticTypes.find(_.hasType(dataType))
    }
  }

  def forName(name: String): StringParameterType[_] = {
    allStaticTypes.find(_.name == name).getOrElse(throw new IllegalArgumentException(s"No parameter type found with name '$name'."))
  }

  object StringType extends StringParameterType[String] {

    override def name: String = "string"

    override def description: String = "A character string."

    def fromString(str: String)(implicit context: PluginContext): String = {
      str
    }

  }

  object CharType extends StringParameterType[Char] {

    override def name: String = "char"

    override def description: String = "A single character."

    def fromString(str: String)(implicit context: PluginContext): Char = {
      if (str.length == 1) {
        str(0)
      }
      else {
        throw new ValidationException("Value must be a single character.")
      }
    }
  }

  object StringIterableParameterType extends StringParameterType[StringIterableParameter] {

    override def name: String = "traversable[string]"

    override def description: String = "A comma-separated list."

    override def fromString(str: String)
                           (implicit context: PluginContext): StringIterableParameter = {
      if(str.isEmpty) {
        StringIterableParameter(Iterable.empty)
      } else {
        StringIterableParameter(str.split("\\s*,\\s*"))
      }
    }

    override def toString(value: StringIterableParameter)
                         (implicit pluginContext: PluginContext): String = {
      value.mkString(", ")
    }
  }

  object IntType extends StringParameterType[Int] {

    override def name: String = "int"

    override def description: String = "An integer number."

    def fromString(str: String)(implicit context: PluginContext): Int = {
      str.toInt
    }

  }

  object LongType extends StringParameterType[Long] {

    override def name: String = "Long"

    override def description: String = "A Long number."

    def fromString(str: String)(implicit context: PluginContext): Long = {
      str.toLong
    }

  }

  object DoubleType extends StringParameterType[Double] {

    override def name: String = "double"

    override def description: String = "A floating-point number."

    def fromString(str: String)(implicit context: PluginContext): Double = {
      str.toDouble
    }

  }

  object BooleanType extends StringParameterType[Boolean] {

    override def name: String = "boolean"

    override def description: String = "Either true or false."

    def fromString(str: String)(implicit context: PluginContext): Boolean = {
      str.toLowerCase match {
        case "true" | "1" => true
        case "false" | "0" => false
        case _ => throw new ValidationException("Value must be either 'true' or 'false'")
      }
    }

  }

  object IntOptionType extends StringParameterType[IntOptionParameter] {

    override def name: String = "option[int]"

    override def description: String = "An optional integer number."

    override def fromString(str: String)(implicit context: PluginContext): IntOptionParameter = {
      if(str.trim.isEmpty) {
        IntOptionParameter(None)
      } else {
        IntOptionParameter(Some(str.toInt))
      }
    }

    override def toString(value: IntOptionParameter)(implicit pluginContext: PluginContext): String = {
      value.value.map(_.toString).getOrElse("")
    }
  }

  object LocaleOptionType extends StringParameterType[LocaleOptionParameter] {

    override def name: String = "option[locale]"

    override def description: String = "An optional locale."

    override def fromString(str: String)(implicit context: PluginContext): LocaleOptionParameter = {
      if (str.trim.isEmpty) {
        LocaleOptionParameter(None)
      } else {
        LocaleOptionParameter(Some(str))
      }
    }

    override def toString(value: LocaleOptionParameter)(implicit pluginContext: PluginContext): String = {
      value.value.getOrElse("")
    }
  }

  object IdentifierOptionType extends StringParameterType[IdentifierOptionParameter] {

    override def name: String = "option[identifier]"

    override def description: String = "An optional Identifier."

    override def fromString(str: String)(implicit context: PluginContext): IdentifierOptionParameter = {
      if(str.trim.isEmpty) {
        None
      } else {
        Some(Identifier(str.trim))
      }
    }

    override def toString(value: IdentifierOptionParameter)(implicit pluginContext: PluginContext): String = {
      value.value.map(_.toString).getOrElse("")
    }
  }

  object StringMapType extends StringParameterType[Map[String, String]] {

    override def name: String = "stringmap"

    override def description: String = "A map of the form 'Key1:Value1,Key2:Value2'"

    private final val utf8: String = "UTF8"

    def fromString(str: String)(implicit context: PluginContext): Map[String, String] = {
      if(str.trim.isEmpty) {
        Map.empty
      } else {
        str.split(',').map(_.split(':')).map(v => Tuple2(URLDecoder.decode(v(0), utf8), URLDecoder.decode(v(1), utf8))).toMap
      }
    }

    override def toString(value: Map[String, String])(implicit pluginContext: PluginContext): String = {
      val strValues = for ((k, v) <- value) yield URLEncoder.encode(k, utf8) + ":" + URLEncoder.encode(v, utf8)
      strValues.mkString(",")
    }

  }

  object UriType extends StringParameterType[Uri] {

    override def name: String = "uri"

    override def description: String = "Either a full URI or a prefixed name."

    def fromString(str: String)(implicit context: PluginContext): Uri = {
      Uri.parse(str, context.prefixes)
    }

    override def toString(value: Uri)(implicit pluginContext: PluginContext): String = {
      value.serialize(pluginContext.prefixes)
    }
  }

  object ResourceType extends StringParameterType[Resource] {

    override def name: String = "resource"

    override def description: String = "Either the name of a project resource or a full URI."

    def fromString(str: String)(implicit context: PluginContext): Resource = {
      if (str.trim.isEmpty) {
        throw new ValidationException("Resource cannot be empty")
      } else {
        context.resources.getInPath(str)
      }
    }

    /**
      * Returns the path relative to the provided project resources.
      *
      * @throws IllegalArgumentException If the project resources are either empty or use a different base path than this resource.
      **/
    override def toString(value: Resource)(implicit pluginContext: PluginContext): String = {
      value.relativePath(pluginContext.resources)
    }
  }

  object WritableResourceType extends StringParameterType[WritableResource] {

    override def name: String = "resource"

    override def description: String = "Either the name of a project resource or a full URI."

    def fromString(str: String)(implicit context: PluginContext): WritableResource = {
      if (str.trim.isEmpty) {
        throw new ValidationException("Resource cannot be empty")
      } else {
        context.resources.getInPath(str)
      }
    }

    override def toString(value: WritableResource)(implicit pluginContext: PluginContext): String = {
      ResourceType.toString(value)
    }
  }

  object ResourceOptionType extends StringParameterType[ResourceOption] {

    override def name: String = "resource"

    override def description: String = "The name of a project resource or empty."

    override def fromString(str: String)(implicit context: PluginContext): ResourceOption = {
      if (str.trim.isEmpty) {
        ResourceOption(None)
      } else {
        ResourceOption(Some(context.resources.getInPath(str)))
      }
    }

    override def toString(value: ResourceOption)(implicit pluginContext: PluginContext): String = {
      value.resource.map(ResourceType.toString).getOrElse("")
    }

  }

  object DurationType extends StringParameterType[Duration] {

    override def name: String = "duration"

    override def description: String = "An amount of time in ISO-8601 duration format PnDTnHnMn.nS"

    override def fromString(str: String)(implicit context: PluginContext): Duration = {
      Duration.parse(str)
    }

    override def toString(value: Duration)(implicit pluginContext: PluginContext): String = {
      value.toString
    }

  }

  object ProjectReferenceType extends StringParameterType[ProjectReference] {

    override def name: String = "project"

    override def description: String = "The identifier of a project in the workspace."

    def fromString(str: String)(implicit context: PluginContext): ProjectReference = {
      ProjectReference(Identifier(str))
    }

    override def toString(value: ProjectReference)(implicit pluginContext: PluginContext): String = {
      value.id
    }

  }

  object TaskReferenceType extends StringParameterType[TaskReference] {

    override def name: String = "task"

    override def description: String = "The identifier of a task in the same project."

    def fromString(str: String)(implicit context: PluginContext): TaskReference = {
      TaskReference(Identifier(str))
    }

    override def toString(value: TaskReference)(implicit pluginContext: PluginContext): String = {
      value.id
    }
  }

  object IdentifierType extends StringParameterType[Identifier] {

    override def name: String = "identifier"

    override def description: String = "The identifier of anything."

    def fromString(str: String)(implicit context: PluginContext): Identifier = {
      Identifier(str)
    }

    override def toString(value: Identifier)(implicit pluginContext: PluginContext): String = {
      value.toString
    }
  }

  object RestrictionType extends StringParameterType[Restriction] {

    override def name: String = "restriction"

    override def description: String = "The restriction of a set of entities."

    def fromString(str: String)(implicit context: PluginContext): Restriction = {
      Restriction.parse(str)(context.prefixes)
    }

    override def toString(value: Restriction)(implicit pluginContext: PluginContext): String = {
      value.serialize
    }
  }

  /** Option enumeration parameter that allows empty strings as None. */
  case class OptionEnumerationType(enumType: Class[_]) extends StringParameterType[Option[Enum[_]]] {
    private val enumerationTypeParameter = EnumerationType(enumType)

    override def fromString(str: String)(implicit context: PluginContext): Option[Enum[_]] = {
      if(str.isEmpty) {
        None
      } else {
        enumerationTypeParameter.fromStringOpt(str)
      }
    }

    override def toString(value: Option[Enum[_]])(implicit pluginContext: PluginContext): String = {
      value match {
        case Some(enum) => enumerationTypeParameter.toString(enum)
        case None => ""
      }
    }

    override def name: String = "option[enumeration]"
  }

  case class EnumerationType(enumType: Class[_]) extends StringParameterType[Enum[_]] {
    require(enumType.isEnum)

    private val enumConstants = enumType.asInstanceOf[Class[Enum[_]]].getEnumConstants

    private val valueList = enumerationValues.mkString(", ")

    override def name: String = "enumeration"

    override def description: String = "One of the following values: " + valueList

    override def fromString(str: String)(implicit context: PluginContext): Enum[_] = {
      fromStringOpt(str) getOrElse (throw new ValidationException(s"Invalid enumeration value '$str'. Allowed values are: $valueList"))
    }

    def fromStringOpt(str: String): Option[Enum[_]] = {
      enumConstants.find {
        case e: EnumerationParameterType =>
          e.id == str.trim || e.name == str.trim
        case c: Enum[_] =>
          c.name == str.trim
      }
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

    override def toString(value: Enum[_])(implicit pluginContext: PluginContext): String = enumerationValue(value)
  }

  object MultilineStringParameterType extends StringParameterType[MultilineStringParameter] {
    override def name: String = "multiline string"

    override def fromString(str: String)(implicit context: PluginContext): MultilineStringParameter = MultilineStringParameter(str)
  }

  object GraphUriParameterType extends StringParameterType[GraphUriParameter] {
    override def name: String = "graph uri"

    override def description: String = "A graph URI."

    override def fromString(str: String)(implicit context: PluginContext): GraphUriParameter = GraphUriParameter(str)
  }

  object TemplateParameterType extends StringParameterType[TemplateParameter] {
    override def name: String = "template"

    override def description: String = "A template."

    override def fromString(str: String)(implicit context: PluginContext): TemplateParameter = TemplateParameter(str)

    override def toString(value: TemplateParameter)(implicit pluginContext: PluginContext): String = value.templateStr
  }

  object PasswordParameterType extends StringParameterType[PasswordParameter] {
    // This preamble should be added to all serializations to mark the string as an encrypted password, else it will be interpreted as plain
    final val PREAMBLE = "PASSWORD_PARAMETER:"
    final val CONFIG_KEY = "plugin.parameters.password.crypt.key"

    override def name: String = "password"

    override def description: String = "A password string."

    /**
      * The configured key. Failure, if no key has been configured or the key is invalid.
      */
    lazy val configuredKey: Try[SecretKey] = {
      for {
        password <- Try(DefaultConfig.instance().getString(CONFIG_KEY))
        checkedPassword <- checkPassword(password)
        key <- Try(AesCrypto.generateKey(checkedPassword))
      } yield key
    }

    /**
      * The key used for encryption. A default key will be used, if no key has been configured.
      */
    lazy val key: SecretKey = {
      configuredKey match {
        case Success(k) => k
        case Failure(ex) =>
          log.log(Level.WARNING, s"No valid key set for $CONFIG_KEY, using insecure default key!", ex)
          AesCrypto.generateKey("1234567890123456")
      }
    }

    override def fromString(str: String)(implicit context: PluginContext): PasswordParameter = {
      val encryptedPassword = if (str == null || str == "") {
        str // Handle empty string as empty password and vice versa
      } else if (str.startsWith(PREAMBLE)) {
        val encryptedPassword = str.stripPrefix(PREAMBLE)
        // Test that it can be decrypted
        PasswordParameter.decrypt(encryptedPassword)
        encryptedPassword
      } else {
        AesCrypto.encrypt(key, str)
      }
      PasswordParameter(encryptedPassword)
    }

    /**
      * Makes sure that the password has been set and is longer than 16 characters.
      */
    private def checkPassword(password: String): Try[String] = {
      if(password ==  "changemechangeme") {
        Failure(new InvalidKeySpecException("Default key is not overridden"))
      } else if(password.length < 16) {
        Failure(new InvalidKeySpecException("Key must be at least 16 characters long"))
      } else {
        Success(password)
      }
    }

  }

  object SparqlEndpointDatasetParameterType extends StringParameterType[SparqlEndpointDatasetParameter] {
    override def name: String = "SPARQL endpoint"

    override def fromString(str: String)(implicit context: PluginContext): SparqlEndpointDatasetParameter = {
      SparqlEndpointDatasetParameter(str)
    }
  }

  trait CodeParameterType[T <: CodeParameter] extends StringParameterType[T] {
    def codeMode: String

    override lazy val name: String = s"code-${codeMode}"
  }

  private object Jinja2CodeParameterType extends CodeParameterType[Jinja2CodeParameter] {
    override def codeMode: String = "jinja2"
    override def fromString(str: String)(implicit context: PluginContext): Jinja2CodeParameter = Jinja2CodeParameter(str)
  }

  private object JsonCodeParameterType extends CodeParameterType[JsonCodeParameter] {
    override def codeMode: String = "json"

    override def fromString(str: String)(implicit context: PluginContext): JsonCodeParameter = JsonCodeParameter(str)
  }

  private object SparqlCodeParameterType extends CodeParameterType[SparqlCodeParameter] {
    override def codeMode: String = "sparql"

    override def fromString(str: String)(implicit context: PluginContext): SparqlCodeParameter = SparqlCodeParameter(str)
  }

  private object SqlCodeParameterType extends CodeParameterType[SqlCodeParameter] {
    override def codeMode: String = "sql"

    override def fromString(str: String)(implicit context: PluginContext): SqlCodeParameter = SqlCodeParameter(str)
  }

  private object XmlCodeParameterType extends CodeParameterType[XmlCodeParameter] {
    override def codeMode: String = "xml"

    override def fromString(str: String)(implicit context: PluginContext): XmlCodeParameter = XmlCodeParameter(str)
  }

  private object YamlCodeParameterType extends CodeParameterType[YamlCodeParameter] {
    override def codeMode: String = "yaml"

    override def fromString(str: String)(implicit context: PluginContext): YamlCodeParameter = YamlCodeParameter(str)
  }

  private object TurtleCodeParameterType extends CodeParameterType[TurtleCodeParameter] {
    override def codeMode: String = "turtle"

    override def fromString(str: String)(implicit context: PluginContext): TurtleCodeParameter = TurtleCodeParameter(str)
  }

  private object PythonCodeParameterType extends CodeParameterType[PythonCodeParameter] {
    override def codeMode: String = "python"

    override def fromString(str: String)(implicit context: PluginContext): PythonCodeParameter = PythonCodeParameter(str)
  }
}
