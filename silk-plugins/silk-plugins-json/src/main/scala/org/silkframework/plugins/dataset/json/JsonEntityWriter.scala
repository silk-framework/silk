package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.{JsonEncoding, JsonFactory}
import org.silkframework.dataset.TypedProperty
import org.silkframework.entity.ValueType
import org.silkframework.plugins.dataset.hierarchical.HierarchicalEntityWriter
import org.silkframework.runtime.validation.ValidationException

import java.io.OutputStream
import java.math.{BigDecimal, BigInteger}

class JsonEntityWriter(outputStream: OutputStream, topLevelObject: Boolean) extends HierarchicalEntityWriter {

  private val generator = (new JsonFactory).createGenerator(outputStream, JsonEncoding.UTF8)

  private var firstEntity = true

  override def startEntity(): Unit = {
    if(firstEntity && topLevelObject) {
      generator.writeStartArray()
    }
    firstEntity = false
    generator.writeStartObject()
  }

  override def endEntity(): Unit = {
    generator.writeEndObject()
  }

  override def startProperty(property: TypedProperty, numberOfValues: Int): Unit = {
    generator.writeFieldName(property.propertyUri)
    if(!property.isAttribute) {
      generator.writeStartArray(numberOfValues)
    } else if(numberOfValues != 1) {
      throw new ValidationException(s"Property ${property.propertyUri} is only allowed to have one value, but got multiple values")
    }
  }

  override def endProperty(property: TypedProperty): Unit = {
    if(!property.isAttribute) {
      generator.writeEndArray()
    }
  }

  override def writeValue(value: Seq[String], property: TypedProperty): Unit = {
    for(v <- value) {
      writeValue(v, property)
    }
  }

  private def writeValue(value: String, property: TypedProperty): Unit = {
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
    if(topLevelObject) {
      generator.writeEndArray()
    }
    generator.close()
  }

}