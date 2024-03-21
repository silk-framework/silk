package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import org.silkframework.runtime.resource.{Resource, ResourceTooLargeException}
import org.silkframework.runtime.validation.ValidationException
import play.api.libs.json._

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ListMap
import scala.collection.mutable.ArrayBuffer

/**
  * JSON reader that keeps track of the current JSON object key and allows to build in-memory nodes.
  */
class JsonReader(parser: JsonParser) extends AutoCloseable {

  private var names: List[String] = List[String]()

  // True, if the current token is a simple value (string, number, etc.)
  private var tokenAtSimpleValue: Boolean = false

  def nextToken(): JsonToken = {
    val prevToken = parser.currentToken()
    val token = parser.nextToken()
    if(tokenAtSimpleValue) {
      names = names.tail
      tokenAtSimpleValue = false
    }
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
      case  JsonToken.VALUE_STRING |
            JsonToken.VALUE_NUMBER_INT |
            JsonToken.VALUE_NUMBER_FLOAT |
            JsonToken.VALUE_FALSE |
            JsonToken.VALUE_TRUE if parser.getCurrentName != null =>
        names ::= parser.getCurrentName
        tokenAtSimpleValue = true
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
  def buildNode(): JsonNode = {
    new NodeBuilder().buildNode()
  }

  /**
   * Closes the underlying parser.
   */
  override def close(): Unit = {
    parser.close()
  }

  private class NodeBuilder {

    private val startByteOffset = parser.getCurrentLocation.getByteOffset

    private val maxSize = Resource.maxInMemorySize()

    /**
      * Builds a JSON node for a given element that includes all its children.
      * On return, the parser will be positioned on the element that directly follows the element.
      */
    def buildNode(): JsonNode = {
      val position = getPosition()
      val value = parser.currentToken match {
        case JsonToken.START_ARRAY =>
          buildArrayNode()
        case JsonToken.START_OBJECT =>
          buildObjectNode()
        case JsonToken.VALUE_STRING =>
          JsonString(parser.getText, position)
        case JsonToken.VALUE_NUMBER_INT |
             JsonToken.VALUE_NUMBER_FLOAT =>
          JsonNumber(parser.getDecimalValue, position)
        case JsonToken.VALUE_TRUE |
             JsonToken.VALUE_FALSE =>
          JsonBoolean(parser.getBooleanValue, position)
        case JsonToken.VALUE_NULL =>
          JsonNull(position)
        case token: JsonToken =>
          throw new ValidationException(s"Unexpected token: $token.")
      }

      // Move to the element after the end element.
      nextTokenSafe()

      value
    }

    private def buildArrayNode(): JsonArray = {
      assert(currentToken == JsonToken.START_ARRAY)
      val position = getPosition()
      nextTokenSafe()
      val children = new ArrayBuffer[JsonNode]()
      while(currentToken != JsonToken.END_ARRAY) {
        children += buildNode()
      }
      JsonArray(children, position)
    }

    private def buildObjectNode(): JsonObject = {
      assert(currentToken == JsonToken.START_OBJECT)
      val position = getPosition()
      nextTokenSafe()
      val children = new ArrayBuffer[(String, JsonNode)]()
      while (currentToken != JsonToken.END_OBJECT) {
        assert(currentToken == JsonToken.FIELD_NAME)
        nextTokenSafe()
        val key = parser.getCurrentName
        val value = buildNode()
        children += (key -> value)
      }
      JsonObject(ListMap() ++ children, position)
    }

    private def getPosition(): JsonPosition = {
      val location = parser.currentTokenLocation
      JsonPosition(location.getLineNr, location.getColumnNr)
    }

    def nextTokenSafe(): Unit = {
      nextToken()
      val currentSize = parser.getCurrentLocation.getByteOffset - startByteOffset
      if(currentSize > maxSize) {
        throw new ResourceTooLargeException("Tried to load an entity into memory that is larger than the configured maximum " +
              s"(bytes read so far: $currentSize, maximum size: $maxSize}). " +
              s"Configure '${Resource.maxInMemorySizeParameterName}' in order to increase this limit.")
      }
    }
  }

}
