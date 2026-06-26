package controllers.workspace.workspaceRequests

import play.api.libs.json.{Format, Json}

/** Batch lookup request for global vocabulary entries.
  *
  * @param projectId Optional project ID used to resolve prefixed names and shorten resolved URIs.
  * @param uris      A list of absolute URIs, prefixed names, or other strings to check against the global vocabulary cache.
  */
case class VocabularyLookupRequest(projectId: Option[String], uris: Seq[String])

object VocabularyLookupRequest {
  implicit val vocabularyLookupRequestJsonFormat: Format[VocabularyLookupRequest] = Json.format[VocabularyLookupRequest]
}

/** Result for a single vocabulary lookup value.
  *
  * @param input       The original input string.
  * @param resolved    True if the value could be resolved to a vocabulary class or property.
  * @param invalid     True if the input could not be parsed as a valid absolute or prefixed URI.
  * @param uri         The resolved absolute URI if valid, otherwise the original input string.
  * @param kind        The resolved vocabulary element kind, either "class" or "property".
  * @param label       The preferred label of the resolved vocabulary element.
  * @param description The description of the resolved vocabulary element.
  * @param prefixedUri The URI shortened with the project or default prefixes, if valid.
  * @param graphUri    The graph URI of the vocabulary element, if available.
  */
case class VocabularyLookupResult(input: String,
                                  resolved: Boolean,
                                  invalid: Boolean,
                                  uri: String,
                                  kind: Option[String] = None,
                                  label: Option[String] = None,
                                  description: Option[String] = None,
                                  prefixedUri: Option[String] = None,
                                  graphUri: Option[String] = None)

object VocabularyLookupResult {
  implicit val vocabularyLookupResultJsonFormat: Format[VocabularyLookupResult] = Json.format[VocabularyLookupResult]
}

/** Response for a batch vocabulary lookup request. */
case class VocabularyLookupResponse(results: Seq[VocabularyLookupResult])

object VocabularyLookupResponse {
  implicit val vocabularyLookupResponseJsonFormat: Format[VocabularyLookupResponse] = Json.format[VocabularyLookupResponse]
}
