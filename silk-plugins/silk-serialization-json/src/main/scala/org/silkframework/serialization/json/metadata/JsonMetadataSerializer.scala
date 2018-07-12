package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.MetadataSerializerRegistry
import org.silkframework.runtime.serialization.SerializationFormat
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json.JsValue

import scala.reflect._

abstract class JsonMetadataSerializer[T : ClassTag] extends JsonFormat[T] with Serializable {

  /**
    * The identifier used to define metadata objects in the map of [[org.silkframework.entity.metadata.EntityMetadata]]
    * NOTE: This method has to be implemented as def and not as val, else the serialization format registration will fail !!!!!!!!!
    */
  def metadataId: String

  //we have to make sure that metadataId was not implemented as a val
  if(runtime.currentMirror.classSymbol(this.getClass).toType.decls.exists(d => d.name.encodedName.toString == "metadataId" && !d.isMethod))
    throw new NotImplementedError("Method metadataId in " + this.getClass.getName + " was implemented as a val. Make sure to implement this method as a def!")

  //add metadata serializer to registry
  //FIXME if in a future version of scala a trait such as 'OnCreation' is introduced, the forcing of implementation
  //methods can be dropped and the following registration can be placed inside such a onCreation method
  JsonMetadataSerializer.registerSerializationFormat(metadataId, this)
}

object JsonMetadataSerializer extends MetadataSerializerRegistry[JsValue] {
  /**
    * Each serialization format needs a dedicated Exception serializer
    */
  override val exceptionSerializer: SerializationFormat[Throwable, JsValue] = ExceptionSerializerJson()
}