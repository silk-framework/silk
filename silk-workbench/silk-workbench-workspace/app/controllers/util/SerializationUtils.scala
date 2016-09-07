package controllers.util

import controllers.workspace.ActivityApi._
import models.JsonError
import org.silkframework.runtime.serialization.Serialization
import play.api.mvc.{AnyContent, Request, Result}

import scala.reflect.ClassTag

object SerializationUtils {

  /**
    * Tries to serialize a given value based on the accept header.
    *
    * @param value The value to be serialized
    * @param defaultMimeType The MIME type to be used if the accept header specifies none or accepts any
    * @param request The HTTP request to be used for content negotiation
    * @return A HTTP result
    */
  def serialize(value: Any, defaultMimeType: String = "application/xml")(implicit request: Request[AnyContent]): Result = {
    val mimeTypes = request.acceptedTypes.map(t => t.mediaType + "/" + t.mediaSubType)
    if (mimeTypes.isEmpty || mimeTypes.contains("*/*")) {
      val serializeValue = Serialization.serialize(value, defaultMimeType)
      Ok(serializeValue).as(defaultMimeType)
    } else {
      mimeTypes.find(Serialization.hasSerialization(value, _)) match {
        case Some(mimeType) =>
          val serializeValue = Serialization.serialize(value, mimeType)
          Ok(serializeValue).as(mimeType)
        case None =>
          NotAcceptable(JsonError(s"No serialization for accepted MIME types available for values of type ${value.getClass.getName}" ))
      }
    }
  }

}
