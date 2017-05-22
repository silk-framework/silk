package controllers.util

import org.silkframework.runtime.serialization.Serialization.serializationFormats
import org.silkframework.runtime.serialization.{ReadContext, Serialization, SerializationFormat, WriteContext}
import org.silkframework.workbench.utils.JsonError
import org.silkframework.workspace.Project
import play.api.http.MediaType
import play.api.libs.json.{JsArray, JsValue}
import play.api.mvc._

import scala.reflect.ClassTag
import scala.xml.{Elem, Node}

object SerializationUtils extends Results {

  private val defaultMimeTypes = Seq("application/xml", "application/json", "text/turtle")

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
                                        defaultMimeTypes: Seq[String] = defaultMimeTypes)
                                       (implicit request: Request[AnyContent],
                                        project: Project): Result = {
    implicit val writeContext = createWriteContext(project)
    val valueType = implicitly[ClassTag[T]].runtimeClass

    applySerializationFormat[T](request.acceptedTypes, defaultMimeTypes, valueType) { (serializationFormat, mimeType) =>
      val serializedValue = serializationFormat.toString(value, mimeType)
      Ok(serializedValue).as(mimeType)
    }
  }

  private def createWriteContext(project: Project) = {
    WriteContext[Any](prefixes = project.config.prefixes)
  }

  /**
    * Tries to serialize a given value based on the accept header. This method uses the runtime class of the submitted value.
    *
    * @param value            The value to be serialized
    * @param defaultMimeTypes The MIME types to be used if the accept header specifies none or accepts any
    * @param request          The HTTP request to be used for content negotiation
    * @param project          The project
    * @return A HTTP result
    */
  def serializeRuntime(value: Any,
                       defaultMimeTypes: Seq[String] = defaultMimeTypes)
                      (implicit request: Request[AnyContent],
                       project: Project): Result = {
    implicit val writeContext = createWriteContext(project)
    val valueType = value.getClass

    val noneType = notAcceptable(request, valueType)
    applySerializationFormat(request.acceptedTypes, defaultMimeTypes, valueType, noneType) { (serializationFormat, mimeType) =>
      val serializedValue = serializationFormat.toString(value, mimeType)
      Ok(serializedValue).as(mimeType)
    }
  }

  private def notAcceptable(request: Request[AnyContent], valueType: Class[_]) = {
    NotAcceptable(JsonError(s"No serialization for accepted MIME types (${request.acceptedTypes.mkString(", ")})" +
        s" available for values of type ${valueType.getSimpleName}"))
  }

  def serializeToStringCompileType[T: ClassTag](value: T,
                                                defaultMimeTypes: Seq[String] = defaultMimeTypes)
                                               (implicit request: Request[AnyContent],
                                                project: Project): Option[String] = {
    implicit val writeContext = createWriteContext(project)
    val valueType = implicitly[ClassTag[T]].runtimeClass

    val noneType = None
    applySerializationFormat[Option[String]](request.acceptedTypes, defaultMimeTypes, valueType, noneType) { (serializationFormat, mimeType) =>
      val serializedValue = serializationFormat.toString(value, mimeType)
      Some(serializedValue)
    }
  }

  def serializeToStringRuntimeType(value: Any,
                                   defaultMimeTypes: Seq[String] = defaultMimeTypes)
                                  (implicit request: Request[AnyContent],
                                   project: Project): Option[String] = {
    implicit val writeContext = createWriteContext(project)
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
                                               (implicit request: Request[AnyContent],
                                                project: Project): Result = {
    implicit val writeContext = createWriteContext(project)
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
        UnsupportedMediaType("Unsupported content type. Try JSON or XML.")
    }
  }

  private def deserializeJsonIterable[T: ClassTag](jsArray: JsArray)
                                                  (implicit readContext: ReadContext): Seq[T] = {
    val deserialized = for (jsObj <- jsArray.value) yield {
      Serialization.formatForType[T, JsValue].read(jsObj)
    }
    deserialized
  }

  private def deserializeXmlIterable[T: ClassTag](expectedRootElementLabel: Option[String],
                                                  rootElem: Elem)
                                                 (implicit readContext: ReadContext): Seq[T] = {
    expectedRootElementLabel.foreach { expected =>
      if (rootElem.label != expected) {
        throw new RuntimeException(s"The root element of the XML document is not the expected! Expected: $expected, got: ${rootElem.label}")
      }
    }
    val deserialized = for (child <- rootElem.child) yield {
      Serialization.formatForType[T, Node].read(child)
    }
    deserialized
  }

  /**
    * Tries to deserialize the value found in the request. Uses the compile type instead of the runtime type.
    *
    * @param defaultMimeType The MIME type to be used if the content-type header specifies none or accepts any
    * @param func            The user provided function to be executed with the parsed value.
    * @param request         The HTTP request to get the value from.
    * @param project         The project
    * @tparam T The expected parsed type.
    * @return A HTTP result. If the serialization succeeds, this will be the result returned by the user-provided function.
    */
  def deserializeCompileTime[T: ClassTag](defaultMimeType: String = "application/xml")
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
        UnsupportedMediaType(JsonError(s"No serialization for content type ${request.mediaType} available for values of type ${valueType.getName}"))
    }
  }

  private def mimeType[T: ClassTag](mediaTypes: Seq[MediaType],
                                    defaultMimeTypes: Seq[String]): Option[String] = {
    val mimeTypes = mediaTypes.map(t => t.mediaType + "/" + t.mediaSubType)
    if (mimeTypes.isEmpty) {
      defaultMimeTypes.find(Serialization.hasSerialization[T])
    } else {
      mimeTypes.find(Serialization.hasSerialization[T])
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
        NotAcceptable(JsonError(s"No serialization for accepted MIME types (${acceptedTypes.mkString(", ")})" +
            s" available for values of type ${classToSerialize.getSimpleName}"))
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
        map(mt => (mt, Serialization.serializationFormat(classToSerialize, mt))).
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
