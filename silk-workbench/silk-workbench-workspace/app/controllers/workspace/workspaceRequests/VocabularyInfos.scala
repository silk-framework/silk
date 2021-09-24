package controllers.workspace.workspaceRequests

import play.api.libs.json.{Format, Json}


/** Vocabulary information.
  *
  * @param uri          URI of the vocabulary (graph).
  * @param label        Preferred label of the vocabulary.
  * @param nrClasses    Number of classes in the vocabulary.
  * @param nrProperties Number of properties in the vocabulary.
  */
case class VocabularyInfo(uri: String, label: Option[String], nrClasses: Int, nrProperties: Int)

object VocabularyInfo {
  implicit val vocabularyInfoJsonFormat: Format[VocabularyInfo] = Json.format[VocabularyInfo]
}
case class VocabularyInfos(vocabularies: Seq[VocabularyInfo])
object VocabularyInfos {
  implicit val vocabularyInfosJsonFormat: Format[VocabularyInfos] = Json.format[VocabularyInfos]
}
