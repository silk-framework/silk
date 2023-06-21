package controllers.linking.evaluation

import play.api.libs.json.{Format, Json}

/** Request to add a path to the reference entities cache.
  *
  * @param path     The path that should be added.
  * @param toTarget If this will be added to the source or target paths.
  */
case class AddPathToReferenceEntitiesCacheRequest(path: String,
                                                  toTarget: Boolean,
                                                  reloadCache: Boolean)

object AddPathToReferenceEntitiesCacheRequest {
  implicit val addPathToReferenceEntitiesCacheRequestFormat: Format[AddPathToReferenceEntitiesCacheRequest] = Json.format[AddPathToReferenceEntitiesCacheRequest]
}
