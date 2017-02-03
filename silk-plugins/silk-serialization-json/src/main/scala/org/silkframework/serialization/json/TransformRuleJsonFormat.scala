package org.silkframework.serialization.json

import org.silkframework.entity.{Path, ValueType}
import org.silkframework.rule.expressions.ExpressionParser
import org.silkframework.rule._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri
import play.api.libs.json.{JsString, JsValue}

/**
  *
  */
object TransformRuleJsonFormat extends JsonFormat[TransformRule] {
  /**
    * Deserializes a value.
    */
  override def read(value: JsValue)(implicit readContext: ReadContext): TransformRule = {
    implicit val prefixes = readContext.prefixes

    (value \ "mappingType").asOpt[JsString] match {
      case Some(JsString("DirectMapping")) =>
        DirectMapping(
          name = (value \ "name").as[JsString].value,
          sourcePath = Path.parse((value \ "source").as[JsString].value),
          mappingTarget =  MappingTargetJsonFormat.read((value \ "mappingTarget").get)
        )
      case Some(JsString("ObjectMapping")) =>
        ObjectMapping(
          name = (value \ "name").as[JsString].value,
          pattern = (value \ "pattern").as[JsString].value,
          targetProperty = Uri.parse((value \ "targetProperty").as[JsString].value, prefixes)
        )
      case Some(JsString("UriMapping")) =>
        UriMapping(
          name = (value \ "name").as[JsString].value,
          pattern = (value \ "pattern").as[JsString].value
        )
      case Some(JsString("TypeMapping")) =>
        TypeMapping(
          name = (value \ "name").as[JsString].value,
          typeUri = Uri.parse((value \ "typeUri").as[JsString].value, prefixes)
        )
      case Some(JsString("ComplexMapping")) =>
        ComplexMapping(
          name = (value \ "name").as[JsString].value,
          operator = ExpressionParser.parse((value \ "sourceExpression").as[JsString].value),
          target = (value \ "mappingTarget").toOption.map(MappingTargetJsonFormat.read)
        )
      case Some(mappingType) =>
        throw new ValidationException(s"Invalid mapping type: $mappingType")
      case None =>
        throw new ValidationException(s"Attribtue 'mappingType' is missing.")
    }
  }

  /**
    * Serializes a value.
    */
  override def write(value: TransformRule)(implicit writeContext: WriteContext[JsValue]): JsValue = ???
}

object MappingTargetJsonFormat extends JsonFormat[MappingTarget] {
  /**
    * Deserializes a value.
    */
  override def read(value: JsValue)(implicit readContext: ReadContext): MappingTarget = {
    MappingTarget(
      propertyUri = Uri.parse((value \ "property").as[JsString].value, readContext.prefixes),
      valueType = ValueType.valueTypeById((value \ "valueType").as[JsString].value).right.get
    )
  }

  /**
    * Serializes a value.
    */
  override def write(value: MappingTarget)(implicit writeContext: WriteContext[JsValue]): JsValue = ???
}