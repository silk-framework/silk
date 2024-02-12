package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.{JsonFactory, JsonGenerator, JsonParser, Version}
import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.{BigIntegerNode, DecimalNode}
import com.fasterxml.jackson.databind.ser.Serializers

import java.io.InputStream
import java.math.{BigInteger, BigDecimal => JBigDec}
import scala.collection.immutable.SeqMap
import scala.collection.mutable

/**
 * A JSON node that stores its source position.
 */
sealed trait JsonNode {

  /**
   * Position in the JSON source.
   */
  def position: JsonPosition

  /**
   * Serialize this node as a JSON string.
   */
  override def toString: String = {
    JsonNodeSerializer.toString(this)
  }

}

case class JsonNull(position: JsonPosition) extends JsonNode

case class JsonBoolean(value: Boolean, position: JsonPosition) extends JsonNode

case class JsonNumber(value: BigDecimal, position: JsonPosition) extends JsonNode

case class JsonString(value: String, position: JsonPosition) extends JsonNode

case class JsonArray(value: scala.collection.IndexedSeq[JsonNode] = Array[JsonNode](), position: JsonPosition) extends JsonNode

case class JsonObject(values: SeqMap[String, JsonNode], position: JsonPosition) extends JsonNode

case class JsonPosition(line: Int, column: Int)

/**
 * (De-)Serializes JSON nodes.
 */
object JsonNodeSerializer {

  private val mapper = (new ObjectMapper).registerModule(new JsonMapperModule())

  /**
   * Parse a JSON string into a node.
   */
  def parse(jsonString: String): JsonNode = {
    buildNode(new JsonFactory().createParser(jsonString))
  }

  /**
   * Parse an incoming JSON stream into a node.
   */
  def parse(inputStream: InputStream): JsonNode = {
    buildNode(new JsonFactory().createParser(inputStream))
  }

  /**
   * Serializes a node as a JSON string.
   */
  def toString(jsonNode: JsonNode): String = {
    mapper.writeValueAsString(jsonNode)
  }

  /**
   * Builds a node from a parser and closes it.
   */
  private def buildNode(parser: JsonParser): JsonNode = {
    val reader = new JsonReader(parser)
    val buffer = mutable.Buffer[JsonNode]()
    try {
      parser.nextToken()
      while(parser.currentToken() != null) {
        buffer.append(reader.buildNode())
      }
    } finally {
      reader.close()
    }
    // For JSON lines, the buffer will contain multiple nodes
    if(buffer.size == 1) {
      buffer.head
    } else {
      JsonArray(buffer.toIndexedSeq, buffer.headOption.map(_.position).getOrElse(JsonPosition(0, 0)))
    }
  }

  private class JsonMapperModule() extends SimpleModule("Silk-JSON", Version.unknownVersion()) {
    override def setupModule(context: SetupContext): Unit = {
      context.addSerializers(new JsonNodeSerializers())
    }
  }

  private class JsonNodeSerializers() extends Serializers.Base {
    override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription): JsonSerializer[_] = {
      if (classOf[JsonNode].isAssignableFrom(beanDesc.getBeanClass)) {
        new JsonNodeSerializer()
      } else {
        null
      }
    }
  }

  private class JsonNodeSerializer() extends JsonSerializer[JsonNode] {
    override def serialize(value: JsonNode, json: JsonGenerator, provider: SerializerProvider): Unit = {
      value match {
        case JsonNumber(v, _) =>
          val str = v.bigDecimal.stripTrailingZeros.toString
          if (str.indexOf('E') < 0 && str.indexOf('.') < 0) {
            json.writeTree(new BigIntegerNode(new BigInteger(str)))
          } else {
            json.writeTree(new DecimalNode(new JBigDec(str)))
          }
        case JsonString(v, _) => json.writeString(v)
        case JsonBoolean(v, _) => json.writeBoolean(v)
        case JsonArray(values, _) =>
          json.writeStartArray()
          values.foreach { t =>
            serialize(t, json, provider)
          }
          json.writeEndArray()
        case JsonObject(values, _) =>
          json.writeStartObject()
          values.foreach { case (key, value) =>
            json.writeFieldName(key)
            serialize(value, json, provider)
          }
          json.writeEndObject()
        case JsonNull(_) => json.writeNull()
      }
    }
  }

}

