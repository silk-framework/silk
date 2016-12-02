package controllers.util

import models.JsonError
import org.silkframework.runtime.serialization.{ReadContext, Serialization, WriteContext}
import org.silkframework.workspace.Project
import play.api.http.MediaType
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.reflect.ClassTag
import scala.xml.Node

object SerializationUtils extends Results {

  /**
    * Tries to serialize a given value based on the accept header.
    *
    * @param value The value to be serialized
    * @param defaultMimeType The MIME type to be used if the accept header specifies none or accepts any
    * @param request The HTTP request to be used for content negotiation
    * @param project The project
    * @return A HTTP result
    */
  def serialize(value: Any, defaultMimeType: String = "application/xml")(implicit request: Request[AnyContent], project: Project): Result = {
    implicit val writeContext = WriteContext[Any](prefixes = project.config.prefixes)

    mimeType(value.getClass, request.acceptedTypes, defaultMimeType) match {
      case Some(mimeType) =>
        val serializeValue = Serialization.formatForMime(value.getClass, mimeType).toString(value, mimeType)
        Ok(serializeValue).as(mimeType)
      case None =>
        NotAcceptable(JsonError(s"No serialization for accepted MIME types available for values of type ${value.getClass.getName}" ))
    }
  }

  /**
    * Tries to deserialize the value found in the request.
    *
    * @param defaultMimeType The MIME type to be used if the content-type header specifies none or accepts any
    * @param func The user provided function to be executed with the parsed value.
    * @param request The HTTP request to get the value from.
    * @param project The project
    * @tparam T The expected parsed type.
    * @return A HTTP result. If the serialization succeeds, this will be the result returned by the user-provided function.
    */
  def deserialize[T: ClassTag](defaultMimeType: String = "application/xml")
                              (func: T => Result)
                              (implicit request: Request[AnyContent], project: Project): Result = {
    val valueType = implicitly[ClassTag[T]].runtimeClass
    implicit val readContext = ReadContext(project.resources, project.config.prefixes)

    mimeType(valueType, request.mediaType.toList, defaultMimeType) match {
      case Some(mimeType) =>
        // Get the data from the body. We optimize the cases for xml and json as Play already parsed these.
        val value =
          request.body match {
            case AnyContentAsXml(xml) => Serialization.formatForType[T, Node].read(xml.head)
            case AnyContentAsJson(json) => Serialization.formatForType[T, JsValue].read(json)
            case AnyContentAsText(str) => Serialization.formatForMime[T](mimeType).fromString(str, mimeType)
            case _ => return UnsupportedMediaType("Unsupported content type")
          }
        // Call the user provided function and return its result
        func(value)
      case None =>
        UnsupportedMediaType(JsonError(s"No serialization for content type ${request.mediaType} available for values of type ${valueType.getName}" ))
    }
  }

  private def mimeType(valueType: Class[_], mediaTypes: Seq[MediaType], defaultMimeType: String): Option[String] = {
    val mimeTypes = mediaTypes.map(t => t.mediaType + "/" + t.mediaSubType)
    if (mimeTypes.isEmpty || mimeTypes.contains("*/*")) {
      Some(defaultMimeType)
    } else {
      mimeTypes.find(Serialization.hasSerialization(valueType, _))
    }
  }

}
