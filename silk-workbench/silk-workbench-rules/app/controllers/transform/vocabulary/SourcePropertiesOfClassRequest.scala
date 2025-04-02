package controllers.transform.vocabulary

import play.api.libs.json.{Format, Json}

/** Request for the properties of class for a RDF source.
  *
  * @param classUri          The class URI the properties should be fetched for.
  * @param fromPathCacheOnly If true, then only properties are returned that were also found in the paths cache for that RDF source.
  * @param includeGeneralProperties If true then also properties defined on owl:Thing and properties without any domain statement are returned.
  */
case class SourcePropertiesOfClassRequest(classUri: String,
                                          fromPathCacheOnly: Boolean,
                                          includeGeneralProperties: Boolean)

object SourcePropertiesOfClassRequest {
  implicit val sourcePropertiesOfClassRequestFormat: Format[SourcePropertiesOfClassRequest] = Json.format[SourcePropertiesOfClassRequest]
}