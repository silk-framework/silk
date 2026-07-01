package controllers.workspace.workspaceRequests

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import play.api.libs.json._

/**
  * Request to update the global vocabulary cache.
  */
@Schema(
  description = "Request to update the global vocabulary cache. It always triggers a general reconciliation of the cache: " +
      "newly installed vocabularies are added and uninstalled ones are removed. Vocabularies already present in the cache are " +
      "not re-fetched by this general update. The optional `iri` forces one or more already-cached vocabularies to be re-fetched."
)
case class UpdateGlobalVocabularyRequest(@Schema(
                                           description = "IRIs of the vocabularies that should be force-reloaded. For each given " +
                                               "IRI the content of that specific vocabulary is re-fetched (the only way to refresh " +
                                               "an already-cached vocabulary), in addition to the general cache update. Accepts " +
                                               "either a single IRI string or an array of IRI strings. If omitted, only the general " +
                                               "update is performed, i.e. newly installed vocabularies are added and uninstalled ones " +
                                               "are removed.",
                                           requiredMode = RequiredMode.NOT_REQUIRED,
                                           nullable = true,
                                           oneOf = Array(classOf[String], classOf[Array[String]])
                                         )
                                         iri: Seq[String] = Seq.empty)

object UpdateGlobalVocabularyRequest {

  /** Reads the `iri` field either as a single string or as an array of strings. A missing or null `iri` results in an
    * empty sequence (general update only). */
  implicit val updateGlobalVocabularyRequestReads: Reads[UpdateGlobalVocabularyRequest] = Reads {
    case obj: JsObject =>
      (obj \ "iri").toOption match {
        case None | Some(JsNull) =>
          JsSuccess(UpdateGlobalVocabularyRequest(Seq.empty))
        case Some(JsString(iri)) =>
          JsSuccess(UpdateGlobalVocabularyRequest(Seq(iri)))
        case Some(array: JsArray) =>
          array.validate[Seq[String]].map(UpdateGlobalVocabularyRequest(_))
        case Some(_) =>
          JsError(JsPath \ "iri", "The 'iri' field must be a string or an array of strings.")
      }
    case _ =>
      JsError("Expected a JSON object.")
  }

  /** Writes the `iri` field: an empty sequence is omitted, a single IRI is written as a string, and multiple IRIs are
    * written as an array. */
  implicit val updateGlobalVocabularyRequestWrites: Writes[UpdateGlobalVocabularyRequest] = Writes { request =>
    request.iri match {
      case Seq() => Json.obj()
      case Seq(singleIri) => Json.obj("iri" -> singleIri)
      case iris => Json.obj("iri" -> iris)
    }
  }

  implicit val updateGlobalVocabularyRequestFormat: Format[UpdateGlobalVocabularyRequest] =
    Format(updateGlobalVocabularyRequestReads, updateGlobalVocabularyRequestWrites)
}
