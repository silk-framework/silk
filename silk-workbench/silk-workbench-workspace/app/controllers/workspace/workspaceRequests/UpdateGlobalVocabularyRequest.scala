package controllers.workspace.workspaceRequests

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import play.api.libs.json.{Format, Json}

/**
  * Request to update the global vocabulary cache.
  *
  * The update always triggers a general reconciliation of the cache: newly installed vocabularies are added and
  * uninstalled ones are removed. Vocabularies already present in the cache are not re-fetched during this general
  * update.
  *
  * @param iri Optional IRI of a single vocabulary that should be force-reloaded. If given, the content of that
  *            specific vocabulary is re-fetched (the only way to refresh an already-cached vocabulary), in addition
  *            to the general update. If omitted, only the general update is performed.
  */
@Schema(
  description = "Request to update the global vocabulary cache. It always triggers a general reconciliation of the cache: " +
      "newly installed vocabularies are added and uninstalled ones are removed. Vocabularies already present in the cache are " +
      "not re-fetched by this general update. The optional `iri` forces a single, already-cached vocabulary to be re-fetched."
)
case class UpdateGlobalVocabularyRequest(@Schema(
                                           description = "Optional IRI of a single vocabulary that should be force-reloaded. " +
                                               "If given, the content of that specific vocabulary is re-fetched (the only way to " +
                                               "refresh an already-cached vocabulary), in addition to the general cache update. " +
                                               "If omitted, only the general update is performed, i.e. newly installed vocabularies " +
                                               "are added and uninstalled ones are removed.",
                                           requiredMode = RequiredMode.NOT_REQUIRED,
                                           nullable = true
                                         )
                                         iri: Option[String] = None)

object UpdateGlobalVocabularyRequest {
  implicit val updateGlobalVocabularyRequestFormat: Format[UpdateGlobalVocabularyRequest] = Json.format[UpdateGlobalVocabularyRequest]
}
