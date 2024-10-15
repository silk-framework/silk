package org.silkframework.serialization.json

import org.silkframework.runtime.resource.{Resource, ResourceManager}
import play.api.libs.json._

/**
 * JSON serializations for resource classes.
 */
object ResourceSerializers {

  def resourceProperties(resource: Resource, resourceManager: ResourceManager): JsValue = {
    val sizeValue = resource.size match {
      case Some(size) => JsNumber(BigDecimal.decimal(size))
      case None => JsNull
    }

    val modificationValue = resource.modificationTime match {
      case Some(time) => JsString(time.toString)
      case None => JsNull
    }

    Json.obj(
      "name" -> resource.name,
      "relativePath" -> JsString(resource.relativePath(resourceManager)),
      "absolutePath" -> resource.path,
      "size" -> sizeValue,
      "modified" -> modificationValue
    )
  }

}
