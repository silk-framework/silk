package org.silkframework.openapi

import com.fasterxml.jackson.databind.`type`.TypeFactory
import io.swagger.v3.core.converter.{AnnotatedType, ModelConverter, ModelConverterContext}
import io.swagger.v3.oas.annotations.media.{Schema => SchemaAnnotation}
import io.swagger.v3.oas.models.media.{ObjectSchema, Schema}
import play.api.libs.json.JsValue

import java.util

/**
  * Documents Play JSON values as free-form JSON objects.
  *
  * Swagger does not know Play's JSON model and would document the internals of JsObject instead, i.e. an object
  * with a required 'underlying' attribute. Annotating the affected attributes does not help, since the generated
  * schema is derived from the attribute type.
  */
class PlayJsonModelConverter extends ModelConverter {

  override def resolve(annotatedType: AnnotatedType,
                       context: ModelConverterContext,
                       chain: util.Iterator[ModelConverter]): Schema[_] = {
    if(isPlayJson(annotatedType)) {
      val schema = new ObjectSchema()
      // The generic resolver does not add the annotated description to a schema that comes from a converter.
      for(description <- annotatedDescription(annotatedType)) {
        schema.setDescription(description)
      }
      schema
    } else if(chain.hasNext) {
      chain.next().resolve(annotatedType, context, chain)
    } else {
      null
    }
  }

  private def isPlayJson(annotatedType: AnnotatedType): Boolean = {
    Option(annotatedType.getType).exists { attributeType =>
      classOf[JsValue].isAssignableFrom(TypeFactory.defaultInstance().constructType(attributeType).getRawClass)
    }
  }

  private def annotatedDescription(annotatedType: AnnotatedType): Option[String] = {
    Option(annotatedType.getCtxAnnotations).toSeq.flatten
      .collectFirst { case schema: SchemaAnnotation => schema.description() }
      .filter(_.nonEmpty)
  }

}
