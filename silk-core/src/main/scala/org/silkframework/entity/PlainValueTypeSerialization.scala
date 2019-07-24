package org.silkframework.entity

import java.text.ParseException

import org.silkframework.runtime.serialization.{ReadContext, SerializationFormat, WriteContext}

import scala.util.{Failure, Success, Try}

object PlainValueTypeSerialization extends SerializationFormat[ValueType, String] {
  /**
    * The MIME types that can be formatted.
    */
  override def mimeTypes: Set[String] = Set("text/plain")

  override def read(value: String)(implicit readContext: ReadContext): ValueType = {
    val (id, instance) = value.trim.span(c => c != '$')
    Try{ValueType.valueTypeById(id.trim) match{
      case Left(c) => c.getSimpleName match{
        case "LanguageValueType" =>
          assert(instance.trim.nonEmpty)
          LanguageValueType(instance.replace("$", "").trim)
        case "CustomValueType" =>
          assert(instance.trim.nonEmpty)
          CustomValueType(instance.replace("$", "").trim)
        case _ => StringValueType  // this should only occur if the old AutoDetectValueType is used which translates to string
      }
      case Right(vt) => vt
    }} match{
      case Success(vt) => vt
      case Failure(_) => throw new ParseException("Could not parse as ValueType: " + value + ". Expected format: 'LanguageValueType$en'", 0)
    }
  }

  override def write(value: ValueType)(implicit writeContext: WriteContext[String]): String = {
    value match{
      case c@CustomValueType(uri) => c.id + "$" + uri
      case c@LanguageValueType(lang) => c.id + "$" + lang
      case d: ValueType => d.id
    }
  }

  /**
    * Formats a value as string.
    */
  override def toString(value: ValueType, mimeType: String)(implicit writeContext: WriteContext[String]): String = write(value)(writeContext)

  /**
    * Formats an iterable of values as string. The optional container name is used for formats where array like values
    * must be named, e.g. XML. This needs to be a valid serialization in the respective data model.
    */
  override def toString(value: Iterable[ValueType], mimeType: String, containerName: Option[String])(implicit writeContext: WriteContext[String]): String = value.mkString(",")

  /**
    * Reads a value from a string.
    */
  override def fromString(value: String, mimeType: String)(implicit readContext: ReadContext): ValueType = read(value)

  /**
    * Read Serialization format from string
    */
  override def parse(value: String, mimeType: String): String = value
}
