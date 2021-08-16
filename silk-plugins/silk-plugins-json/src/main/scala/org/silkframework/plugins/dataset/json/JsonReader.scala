package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import org.silkframework.runtime.validation.ValidationException
import play.api.libs.json._

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.collection.mutable.ArrayBuffer

/**
  * JSON reader that keeps track of the current JSON object key and allows to build in-memory nodes.
  */
class JsonReader(parser: JsonParser) extends AutoCloseable {

  private var names: List[String] = List[String]()

  def nextToken(): JsonToken = {
    val prevToken = parser.currentToken()
    val token = parser.nextToken()
    token match {
      case JsonToken.START_OBJECT |
           JsonToken.START_ARRAY if prevToken == JsonToken.FIELD_NAME =>
        names ::= parser.getCurrentName
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
    parser.currentToken()
  }

  def currentName: String = {
    names.headOption.getOrElse("")
  }

  def currentNameEncoded: String = {
    URLEncoder.encode(currentName, StandardCharsets.UTF_8.name)
  }

  def hasCurrentToken: Boolean = {
    parser.hasCurrentToken
  }

  /**
    * Builds a JSON node for a given element that includes all its children.
    * On return, the parser will be positioned on the element that directly follows the element.
    */
  def buildNode(): JsValue = {
    val value = parser.currentToken match {
      case JsonToken.START_ARRAY =>
        buildArrayNode()
      case JsonToken.START_OBJECT =>
        buildObjectNode()
      case JsonToken.VALUE_STRING =>
        JsString(parser.getText)
      case JsonToken.VALUE_NUMBER_INT |
           JsonToken.VALUE_NUMBER_FLOAT =>
        JsNumber(parser.getDecimalValue)
      case JsonToken.VALUE_TRUE |
           JsonToken.VALUE_FALSE =>
        JsBoolean(parser.getBooleanValue)
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
      val key = parser.getCurrentName
      val value = buildNode()
      children += (key -> value)
    }
    JsObject(children.toMap)
  }

  override def close(): Unit = {
    parser.close()
  }
}
