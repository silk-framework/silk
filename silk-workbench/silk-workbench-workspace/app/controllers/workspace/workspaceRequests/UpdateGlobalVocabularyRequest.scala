package controllers.workspace.workspaceRequests

import play.api.libs.json.{Format, Json}

/**
  * Request to update a specific vocabulary.
  */
case class UpdateGlobalVocabularyRequest(iri: String)

object UpdateGlobalVocabularyRequest {
  implicit val updateGlobalVocabularyRequestFormat: Format[UpdateGlobalVocabularyRequest] = Json.format[UpdateGlobalVocabularyRequest]
}
