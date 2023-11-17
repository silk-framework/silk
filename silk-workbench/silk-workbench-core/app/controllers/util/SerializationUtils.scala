package controllers.util


import org.silkframework.config.Prefixes
import org.silkframework.runtime.serialization.{ReadContext, Serialization, SerializationFormat, WriteContext}
import org.silkframework.runtime.validation.{BadUserInputException, ValidationException}
import org.silkframework.workbench.utils.{ErrorResult, NotAcceptableException}
import org.silkframework.workspace.Project
import play.api.http.MediaType
import play.api.http.Status._
import play.api.libs.json.{JsArray, JsValue}
import play.api.mvc.Results.Ok
import play.api.mvc._
import Serialization.defaultMimeTypes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.EmptyResourceManager

import scala.reflect.ClassTag
import scala.xml.{Elem, Node}

object SerializationUtils {
  final val APPLICATION_JSON = "application/json"
  final val APPLICATION_XML = "application/xml"

  /**
    * Tries to serialize a given value based on the accept header. The compile time type is used instead of the runtime
    * type of the submitted value. Setting the class tag explicitly will search a serialization format for this exact type.
    *
    * @param value            The value to be serialized
    * @param defaultMimeTypes The MIME types to be used if the accept header specifies none or accepts any
    * @param request          The HTTP request to be used for content negotiation
    * @param project          The project
    * @return A HTTP result
    */
  def serializeCompileTime[T: ClassTag](value: T,
                                        project: Option[Project],
                                        defaultMimeTypes: Seq[String] = defaultMimeTypes)
                                       (implicit request: Request[AnyContent]): Result = {
    implicit val writeContext: WriteContext[Any] = createWriteContext(project)
    val valueType = implicitly[ClassTag[T]].runtimeClass

    applySerializationFormat[T](request.acceptedTypes, defaultMimeTypes, valueType) { (serializationFormat, mimeType) =>
      val serializedValue = serializationFormat.toString(value, mimeType)
      Ok(serializedValue).as(mimeType)
    }
  }

  private def createWriteContext(project: Option[Project]): WriteContext[Any] = {
    project match {
      case Some(proj) =>
        WriteContext.fromProject[Any](proj)(UserContext.Empty)
      case None =>
        WriteContext[Any](prefixes = Prefixes.default, projectId = None, resources = EmptyResourceManager(), user = UserContext.Empty)
    }
  }

  /**
    * Tries to serialize a given value based on the accept header. This method uses the runtime class of the submitted value.
    *
    * @param value            The value to be serialized
    * @param defaultMimeTypes The MIME types to be used if the accept header specifies none or accepts any
    * @param request          The HTTP request to be used for content negotiation
    * @param projectOpt       Optional project as context
    * @return A HTTP result
    */
  def serializeRuntime(value: Any,
                       defaultMimeTypes: Seq[String] = defaultMimeTypes)
                      (implicit request: Request[AnyContent],
                       projectOpt: Option[Project]): Result = {
    implicit val writeContext: WriteContext[Any] = createWriteContext(projectOpt)
    val valueType = value.getClass

    val noneType = notAcceptable(request, valueType)
    applySerializationFormat(request.acceptedTypes, defaultMimeTypes, valueType, noneType) { (serializationFormat, mimeType) =>
      val serializedValue = serializationFormat.toString(value, mimeType)
      Ok(serializedValue).as(mimeType)
    }
  }

  private def notAcceptable(request: Request[AnyContent], valueType: Class[_]) = {
    val msg = s"No serialization for accepted MIME types (${request.acceptedTypes.mkString(", ")})" +
      s" available for values of type ${valueType.getSimpleName}"
    ErrorResult(NotAcceptableException(msg))
  }

  def serializeToStringCompileType[T: ClassTag](value: T,
                                                defaultMimeTypes: Seq[String] = defaultMimeTypes)
                                               (implicit request: Request[AnyContent],
                                                project: Project): Option[String] = {
    implicit val writeContext = createWriteContext(Some(project))
    val valueType = implicitly[ClassTag[T]].runtimeClass

    val noneType = None
    applySerializationFormat[Option[String]](List(), defaultMimeTypes, valueType, noneType) { (serializationFormat, mimeType) =>
      val serializedValue = serializationFormat.toString(value, mimeType)
      Some(serializedValue)
    }
  }

  def serializeToStringRuntimeType(value: Any,
                                   defaultMimeTypes: Seq[String] = defaultMimeTypes)
                                  (implicit request: Request[AnyContent],
                                   project: Project): Option[String] = {
    implicit val writeContext = createWriteContext(Some(project))
    val valueType = value.getClass

    val noneType = None
    applySerializationFormat[Option[String]](request.acceptedTypes, defaultMimeTypes, valueType, noneType) { (serializationFormat, mimeType) =>
      val serializedValue = serializationFormat.toString(value, mimeType)
      Some(serializedValue)
    }
  }

  def serializeIterableCompileTime[T: ClassTag](value: Iterable[T],
                                                defaultMimeTypes: Seq[String] = defaultMimeTypes,
                                                containerName: Option[String] = None)
                                               (implicit request: RequestHeader,
                                                project: Project): Result = {
    implicit val writeContext = createWriteContext(Some(project))
    val valueType = implicitly[ClassTag[T]].runtimeClass

    applySerializationFormat[T](request.acceptedTypes, defaultMimeTypes, valueType) { (serializationFormat, mimeType) =>
      val serializedValue = serializationFormat.toString(value, mimeType, containerName)
      Ok(serializedValue).as(mimeType)
    }
  }

  /**
    * Deserializes the body content based on the content type and calls the caller given function on the deserialized result.
    *
    * @param expectedXmlRootElementLabel An optional string that specifies which label the enclosing XML root element should have.
    *                                    If the label is different the method will throw an exception. This only makes sense
    *                                    for data models where list elements are named, e.g. XML.
    * @param func                        User defined function that should be called on the deserialized result.
    */
  def deserializeIterable[T: ClassTag](expectedXmlRootElementLabel: Option[String])
                                      (func: Iterable[T] => Result)
                                      (implicit request: Request[AnyContent],
                                       project: Project): Result = {
    implicit val readContext = ReadContext(project.resources, project.config.prefixes)

    request.body match {
      case AnyContentAsXml(xml) =>
        xml.headOption match {
          case Some(rootElem: Elem) =>
            func(deserializeXmlIterable(expectedXmlRootElementLabel, rootElem))
          case _ =>
            throw new RuntimeException("XML document has no root element!")
        }
      case AnyContentAsJson(json) =>
        json match {
          case jsArray: JsArray =>
            func(deserializeJsonIterable(jsArray))
          case _ =>
            throw new RuntimeException("Received JSON data is not a JSON array!")
        }
      case _ =>
        ErrorResult(UNSUPPORTED_MEDIA_TYPE, title = "Unsupported media type", detail = "Unsupported content type. Try JSON or XML.")
    }
  }

  def deserializeJsonIterable[T: ClassTag](jsArray: JsArray)
                                          (implicit readContext: ReadContext): Seq[T] = {
    try {
      val deserialized = for (jsObj <- jsArray.value.toSeq) yield {
        Serialization.formatForType[T, JsValue].read(jsObj)
      }
      deserialized
    } catch {
      case v: ValidationException =>
        throw BadUserInputException(v.getMessage)
    }
  }

  private def deserializeXmlIterable[T: ClassTag](expectedRootElementLabel: Option[String],
                                                  rootElem: Elem)
                                                 (implicit readContext: ReadContext): Seq[T] = {
    expectedRootElementLabel.foreach { expected =>
      if (rootElem.label != expected) {
        throw new RuntimeException(s"The root element of the XML document is not the expected! Expected: $expected, got: ${rootElem.label}")
      }
    }
    try {
      val deserialized = for (child <- rootElem.child) yield {
        Serialization.formatForType[T, Node].read(child)
      }
      deserialized
    } catch {
      case v: ValidationException =>
        throw BadUserInputException(v.getMessage)
    }
  }

  /**
    * Tries to deserialize the value found in the request. Uses the compile type instead of the runtime type.
    *
    * @param defaultMimeType The MIME type to be used if the content-type header specifies none or accepts any
    * @param func            The user provided function to be executed with the parsed value.
    * @param request         The HTTP request to get the value from.
    * @tparam T The expected parsed type.
    * @return A HTTP result. If the serialization succeeds, this will be the result returned by the user-provided function.
    */
  def deserializeCompileTime[T: ClassTag](defaultMimeType: String = APPLICATION_XML)
                                         (func: T => Result)
                                         (implicit request: Request[AnyContent], readContext: ReadContext): Result = {
    val valueType = implicitly[ClassTag[T]].runtimeClass

    try {
      mimeType(request.mediaType.toList, Seq(defaultMimeType)) match {
        case Some(mimeType) =>
          // Get the data from the body. We optimize the cases for xml and json as Play already parsed these.
          val value =
            request.body match {
              case AnyContentAsXml(xml) => Serialization.formatForType[T, Node].read(xml.head)
              case AnyContentAsJson(json) => Serialization.formatForType[T, JsValue].read(json)
              case AnyContentAsText(str) => Serialization.formatForMime[T](mimeType).fromString(str, mimeType)
              case AnyContentAsRaw(raw) =>
                raw.asBytes() match {
                  case Some(bytes) =>
                    val str = new String(bytes.toArrayUnsafe(), request.charset.getOrElse("UTF8"))
                    Serialization.formatForMime[T](mimeType).fromString(str, mimeType)
                  case None =>
                    return ErrorResult(BAD_REQUEST, title = "Body too large", detail = "The raw body is too large to be loaded into memory.")
                }
              case _ => return ErrorResult(UNSUPPORTED_MEDIA_TYPE, title = "Unsupported Media Type", detail = "Unsupported content type")
            }
          // Call the user provided function and return its result
          func(value)
        case None =>
          val msg = s"No serialization for content type ${request.mediaType} available for values of type ${valueType.getName}"
          ErrorResult(UNSUPPORTED_MEDIA_TYPE, title = "Unsupported Media Type", detail = msg) }
    } catch {
      case v: ValidationException =>
        throw BadUserInputException(v.getMessage)
    }
  }

  private def mimeType[T: ClassTag](mediaTypes: Seq[MediaType],
                                    defaultMimeTypes: Seq[String]): Option[String] = {
    val mimeTypes = mediaTypes.map(t => t.mediaType + "/" + t.mediaSubType)
    if (mimeTypes.isEmpty) {
      defaultMimeTypes.find(Serialization.hasFormatFormMime[T])
    } else {
      mimeTypes.find(Serialization.hasFormatFormMime[T])
    }
  }

  /** Fetch an appropriate serialization format and execute the function parameter on it and the handled MIME type string. */
  private def applySerializationFormat[T: ClassTag](acceptedTypes: Seq[MediaType],
                                                    defaultMimeTypes: Seq[String],
                                                    classToSerialize: Class[_])
                                                   (fn: (SerializationFormat[Any, Any], String) => Result) = {
    mimeType[T](acceptedTypes, defaultMimeTypes) match {
      case Some(mimeType) =>
        fn(Serialization.formatForMime(classToSerialize, mimeType), mimeType)
      case None =>
        val msg = s"No serialization for accepted MIME types (${acceptedTypes.mkString(", ")})" +
          s" available for values of type ${classToSerialize.getSimpleName}"
        ErrorResult(NotAcceptableException(msg))
    }
  }

  // Returns the serializer and the matched MIME type, if nothing matches it returns None
  private def serializerByMimeType(classToSerialize: Class[_],
                                   mediaTypes: Seq[MediaType],
                                   defaultMimeTypes: Seq[String]): Option[(String, SerializationFormat[Any, Any])] = {
    val mimeTypes = mediaTypes.map(t => t.mediaType + "/" + t.mediaSubType)
    if (mimeTypes.isEmpty || mimeTypes.contains("*/*")) {
      findSerializationFormatByMimeType(classToSerialize, defaultMimeTypes)
    } else {
      findSerializationFormatByMimeType(classToSerialize, mimeTypes)
    }
  }

  private def findSerializationFormatByMimeType(classToSerialize: Class[_], mimeTypes: Seq[String]) = {
    mimeTypes.
      map(mt => (mt, Serialization.formatForMimeOption(classToSerialize, mt))).
      find { case (mimeType, serializeFormat) => serializeFormat.isDefined }.
      map(tuple => (tuple._1, tuple._2.get))
  }

  private def applySerializationFormat[T](acceptedTypes: Seq[MediaType],
                                          defaultMimeTypes: Seq[String],
                                          classToSerialize: Class[_],
                                          noneValue: T)
                                         (fn: (SerializationFormat[Any, Any], String) => T): T = {
    serializerByMimeType(classToSerialize, acceptedTypes, defaultMimeTypes) match {
      case Some((mimeType, serializationFormat)) =>
        fn(serializationFormat, mimeType)
      case None =>
        noneValue
    }
  }
}
