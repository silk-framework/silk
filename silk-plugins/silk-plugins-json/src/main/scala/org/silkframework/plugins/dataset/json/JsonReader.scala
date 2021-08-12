package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.{JsonFactory, JsonParser, JsonToken}
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.validation.ValidationException
import play.api.libs.json._

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.collection.mutable.ArrayBuffer

class JsonReader(resource: Resource) {

  private val reader = initStreamReader()

  private var names: List[String] = List[String]()

  nextToken()

  def nextToken(): JsonToken = {
    val prevToken = reader.currentToken()
    val token = reader.nextToken()
    token match {
      case JsonToken.START_OBJECT |
           JsonToken.START_ARRAY if prevToken == JsonToken.FIELD_NAME =>
        names ::= reader.getCurrentName
      case JsonToken.START_OBJECT |
           JsonToken.START_ARRAY if names.nonEmpty =>
        names ::= names.head
      case JsonToken.END_OBJECT |
           JsonToken.END_ARRAY if names.nonEmpty =>
        names = names.tail
      case _ =>
    }
    token
  }

  def currentToken: JsonToken = {
    reader.currentToken()
  }

  def currentName: String = {
    names.headOption.getOrElse("")
  }

  def currentNameEncoded: String = {
    URLEncoder.encode(currentName, StandardCharsets.UTF_8.name)
  }

  def hasCurrentToken: Boolean = {
    reader.hasCurrentToken
  }

  private def initStreamReader(): JsonParser = {
    val factory = new JsonFactory()
    val parser = factory.createParser(resource.inputStream)
    parser
    // TODO close reader
  }

  /**
    * Builds a JSON node for a given start element that includes all its children.
    * The parser must be positioned on the start element when calling this method.
    * On return, the parser will be positioned on the element that directly follows the element.
    */
  def buildNode(): JsValue = {
    val value = reader.currentToken match {
      case JsonToken.START_ARRAY =>
        buildArrayNode()
      case JsonToken.START_OBJECT =>
        buildObjectNode()
      case JsonToken.VALUE_STRING =>
        JsString(reader.getText)
      case JsonToken.VALUE_NUMBER_INT |
           JsonToken.VALUE_NUMBER_FLOAT =>
        JsNumber(reader.getDecimalValue)
      case JsonToken.VALUE_TRUE |
           JsonToken.VALUE_FALSE =>
        JsBoolean(reader.getBooleanValue)
      case JsonToken.VALUE_NULL =>
        JsNull
      case token: JsonToken =>
        throw new ValidationException(s"Unexpected token: $token.")
    }

    // Move to the element after the end element.
    nextToken()

    value
  }

  private def buildArrayNode(): JsArray = {
    assert(currentToken == JsonToken.START_ARRAY)
    nextToken()
    val children = new ArrayBuffer[JsValue]()
    while(currentToken != JsonToken.END_ARRAY) {
      children += buildNode()
    }
    JsArray(children)
  }

  private def buildObjectNode(): JsObject = {
    assert(currentToken == JsonToken.START_OBJECT)
    nextToken()
    val children = new ArrayBuffer[(String, JsValue)]()
    while(currentToken != JsonToken.END_OBJECT) {
      assert(currentToken == JsonToken.FIELD_NAME)
      nextToken()
      val key = reader.getCurrentName
      val value = buildNode()
      children += (key -> value)
    }
    JsObject(children.toMap)
  }

}
