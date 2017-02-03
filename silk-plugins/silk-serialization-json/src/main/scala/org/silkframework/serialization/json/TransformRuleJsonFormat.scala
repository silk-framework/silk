package org.silkframework.serialization.json

import org.silkframework.entity.{Path, ValueType}
import org.silkframework.rule.expressions.{ExpressionParser, Expressions}
import org.silkframework.rule.{ComplexMapping, DirectMapping, MappingTarget, TransformRule}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.util.Uri
import play.api.libs.json.{JsString, JsValue}

/**
  * Created by risele on 2/3/2017.
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
      case Some(JsString("ComplexMapping")) =>
        ComplexMapping(
          name = (value \ "name").as[JsString].value,
          operator = ExpressionParser.parse((value \ "sourceExpression").as[JsString].value),
          target = (value \ "mappingTarget").toOption.map(MappingTargetJsonFormat.read)
        )
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