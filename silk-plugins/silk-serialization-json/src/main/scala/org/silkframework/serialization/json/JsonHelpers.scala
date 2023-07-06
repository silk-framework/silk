package org.silkframework.serialization.json

import java.time.Instant
import java.time.format.DateTimeParseException

import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import play.api.libs.json._

/**
  * Helper methods for handling (Play) JSON.
  */
object JsonHelpers {

  final val ID = "id"

  def mustBeDefined(value: JsValue, attributeName: String): JsValue = {
    (value \ attributeName).toOption.
      getOrElse(throw JsonParseException("No attribute with name '" + attributeName + "' found!"))
  }

  def mustBeJsObject[T](jsValue: JsValue)(block: JsObject => T): T = {
    jsValue match {
      case null => null.asInstanceOf[T]             //valid object representation
      case jsObject: JsObject => block(jsObject)
      case _ => throw JsonParseException("Error while parsing. JSON value is not JSON object!")
    }
  }

  def mustBeJsArray[T](jsValue: JsValue)(block: JsArray => T): T = {
    jsValue match {
      case null => null.asInstanceOf[T]             //valid array representation
      case jsArray: JsArray => block(jsArray)
      case _ => throw JsonParseException("Error while parsing. JSON value is not a JSON array!")
    }
  }

  // Expects either a JSON array or Null.
  def mustBeOptionalJsArray[T](jsValue: JsValue)(block: Option[JsArray] => T): T = {
    jsValue match {
      case null => block(None) //valid array representation
      case jsArray: JsArray => block(Some(jsArray))
      case JsNull => block(None)
      case _ => throw JsonParseException("Error while parsing. JSON value is not a JSON array nor Null!")
    }
  }

  def mustBeOptionalJsArray[T](jsLookupResult: JsLookupResult)(block: Option[JsArray] => T): T = {
    jsLookupResult match {
      case JsDefined(value) => mustBeOptionalJsArray(value)(block)
      case _: JsUndefined => block(None)
    }
  }

  def stringValue(json: JsValue, attributeName: String): String = {
    stringValueOption(json, attributeName) match {
      case Some(value) =>
        value
      case None =>
        throw JsonParseException("Attribute '" + attributeName + "' not found!")
    }
  }

  def booleanValue(json: JsValue, attributeName: String): Boolean = {
    requiredValue(json, attributeName) match {
      case JsBoolean(value) =>
        value
      case _ =>
        throw JsonParseException("Attribute '" + attributeName + "' must be a boolean!")
    }
  }

  def numberValue(json: JsValue, attributeName: String): BigDecimal = {
    requiredValue(json, attributeName) match {
      case JsNumber(value) =>
        value
      case JsString(value) =>
        BigDecimal(value)
      case _ =>
        throw JsonParseException("Attribute '" + attributeName + "' must be a number!")
    }
  }

  def booleanValueOption(json: JsValue, attributeName: String): Option[Boolean] = {
    optionalValue(json, attributeName) match {
      case Some(jsBoolean: JsBoolean) =>
        Some(jsBoolean.value)
      case Some(jsString: JsString) =>
        Some(jsString.value.toBoolean)
      case Some(_) =>
        throw JsonParseException("Value for attribute '" + attributeName + "' is not a boolean!")
      case None =>
        None
    }
  }

  def stringValueOption(json: JsValue, attributeName: String): Option[String] = {
    optionalValue(json, attributeName) match {
      case Some(jsString: JsString) =>
        Some(jsString.value)
      case Some(_) =>
        throw JsonParseException("Value for attribute '" + attributeName + "' is not a String!")
      case None =>
        None
    }
  }

  def numberValueOption(json: JsValue, attributeName: String): Option[BigDecimal] = {
    optionalValue(json, attributeName) match {
      case Some(JsNumber(value)) =>
        Some(value)
      case Some(JsString(value)) =>
        Some(BigDecimal(value))
      case Some(_) =>
        throw JsonParseException("Value for attribute '" + attributeName + "' is not a number!")
      case None =>
        None
    }
  }

  def instantValueOption(json: JsValue, attributeName: String): Option[Instant] = {
    optionalValue(json, attributeName) match {
      case Some(JsString(value)) =>
        try {
          Some(Instant.parse(value))
        } catch {
          case ex: DateTimeParseException =>
            throw JsonParseException(s"Value for attribute '$attributeName' is no valid ISO-8601 instant (e.g., 2020-12-03T10:15:30.00Z)", Some(ex))
        }
      case Some(_) =>
        throw JsonParseException("Value for attribute '" + attributeName + "' must a string.")
      case None =>
        None
    }
  }

  def arrayValueOption(json: JsValue, attributeName: String): Option[JsArray] = {
    optionalValue(json, attributeName) match {
      case Some(jsArray @ JsArray(_)) =>
        Some(jsArray)
      case Some(_) =>
        throw JsonParseException("Value for attribute '" + attributeName + "' is not a JSON array!")
      case None =>
        None
    }
  }

  def arrayValue(json: JsValue, attributeName: String): JsArray = {
    requiredValue(json, attributeName) match {
      case array: JsArray => array
      case _ =>
        throw JsonParseException("Value for attribute '" + attributeName + "' is not a JSON array!")
    }
  }

  def objectValue(json: JsValue, attributeName: String): JsObject = {
    requiredValue(json, attributeName) match {
      case jsObject: JsObject => jsObject
      case _ =>
        throw JsonParseException("Value for attribute '" + attributeName + "' is not a JSON object!")
    }
  }

  def optionalValue(json: JsValue, attributeName: String): Option[JsValue] = {
    (json \ attributeName).toOption.filterNot(_ == JsNull)
  }

  def requiredValue(json: JsValue, attributeName: String): JsValue = {
    json \ attributeName match {
      case JsDefined(value) if value != JsNull =>
        value
      case _ =>
        throw JsonParseException("Attribute '" + attributeName + "' not found!")
    }
  }

  def silkPath(id: String, pathStr: String)(implicit readContext: ReadContext): UntypedPath = {
    try {
      UntypedPath.parse(pathStr)(readContext.prefixes)
    } catch {
      case ex: Exception => throw new ValidationException(ex.getMessage, id, "path")
    }
  }

  def identifier(json: JsValue, defaultId: String)(implicit readContext: ReadContext): Identifier = {
    optionalValue(json, ID) match {
      case Some(JsString(id)) =>
        id
      case Some(_) =>
        throw JsonParseException("Value for attribute '" + ID + "' is not a String!")
      case None =>
        readContext.identifierGenerator.generate(defaultId)
    }
  }

  /** Returns the parsed object or throws a JsonParseException. */
  def fromJsonValidated[T](json: JsValue)
                          (implicit reads: Reads[T]): T = {
    Json.fromJson[T](json) match {
      case JsSuccess(obj, _) =>
        obj
      case JsError(errors) =>
        throw JsonParseException("JSON parse error. Details: " + JsError.toJson(errors).toString())
    }
  }
}
