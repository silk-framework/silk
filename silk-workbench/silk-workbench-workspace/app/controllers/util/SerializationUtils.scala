package controllers.util

import org.silkframework.runtime.serialization.{ReadContext, Serialization, WriteContext}
import org.silkframework.workbench.utils.JsonError
import org.silkframework.workspace.Project
import play.api.http.MediaType
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.reflect.ClassTag
import scala.xml.Node

object SerializationUtils extends Results {

  private val defaultMimeTypes = Seq("application/xml", "application/json", "text/turtle")

  /**
    * Tries to serialize a given value based on the accept header.
    *
    * @param value The value to be serialized
    * @param defaultMimeTypes The MIME types to be used if the accept header specifies none or accepts any
    * @param request The HTTP request to be used for content negotiation
    * @param project The project
    * @return A HTTP result
    */
  def serialize[T: ClassTag](value: T, defaultMimeTypes: Seq[String] = defaultMimeTypes)(implicit request: Request[AnyContent], project: Project): Result = {
    implicit val writeContext = WriteContext[Any](prefixes = project.config.prefixes)
    val valueType = implicitly[ClassTag[T]].runtimeClass

    mimeType[T](request.acceptedTypes, defaultMimeTypes) match {
      case Some(mimeType) =>
        val serializeValue = Serialization.formatForMime(valueType, mimeType).toString(value, mimeType)
        Ok(serializeValue).as(mimeType)
      case None =>
        NotAcceptable(JsonError(s"No serialization for accepted MIME types (${request.acceptedTypes.mkString(", ")}) available for values of type ${value.getClass.getName}" ))
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

    mimeType(request.mediaType.toList, Seq(defaultMimeType)) match {
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

  private def mimeType[T: ClassTag](mediaTypes: Seq[MediaType], defaultMimeTypes: Seq[String]): Option[String] = {
    val mimeTypes = mediaTypes.map(t => t.mediaType + "/" + t.mediaSubType)
    if (mimeTypes.isEmpty || mimeTypes.contains("*/*")) {
      defaultMimeTypes.find(Serialization.hasSerialization[T](_))
    } else {
      mimeTypes.find(Serialization.hasSerialization[T](_))
    }
  }

}
