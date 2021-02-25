package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.{JsonEncoding, JsonFactory}
import org.silkframework.dataset.TypedProperty
import org.silkframework.entity.ValueType
import org.silkframework.plugins.dataset.hierarchical.HierarchicalEntityWriter

import java.io.OutputStream
import java.math.{BigDecimal, BigInteger}

class JsonEntityWriter(outputStream: OutputStream) extends HierarchicalEntityWriter {

  private val generator = (new JsonFactory).createGenerator(outputStream, JsonEncoding.UTF8)

  override def startEntity(): Unit = {
    generator.writeStartObject()
  }

  override def endEntity(): Unit = {
    generator.writeEndObject()
  }

  override def startArray(size: Int): Unit = {
    generator.writeStartArray(size)
  }

  override def endArray(): Unit = {
    generator.writeEndArray()
  }

  override def writeField(property: TypedProperty): Unit = {
    generator.writeFieldName(property.propertyUri)
  }

  override def writeValue(value: Seq[String], property: TypedProperty): Unit = {
    if(value.size == 1) {
      writeValue(value.head, property)
    } else {
      generator.writeStartArray(value.size)
      for(v <- value) {
        writeValue(v, property)
      }
      generator.writeEndArray()
    }
  }

  override def writeValue(value: String, property: TypedProperty): Unit = {
    property.valueType match {
      case ValueType.INTEGER =>
        generator.writeNumber(new BigInteger(value))
      case ValueType.INT =>
        generator.writeNumber(value.toInt)
      case ValueType.LONG =>
        generator.writeNumber(value.toLong)
      case ValueType.FLOAT =>
        generator.writeNumber(value.toFloat)
      case ValueType.DOUBLE =>
        generator.writeNumber(value.toDouble)
      case ValueType.DECIMAL =>
        generator.writeNumber(new BigDecimal(value))
      case ValueType.BOOLEAN =>
        generator.writeBoolean(value.toBoolean)
      case _ =>
        generator.writeString(value)
    }
  }

  override def close(): Unit = {
    generator.close()
  }

}