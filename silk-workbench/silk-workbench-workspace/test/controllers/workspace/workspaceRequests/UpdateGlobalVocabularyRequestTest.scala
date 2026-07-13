package controllers.workspace.workspaceRequests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsResult, Json}

class UpdateGlobalVocabularyRequestTest extends AnyFlatSpec with Matchers {

  behavior of "UpdateGlobalVocabularyRequest JSON reads"

  private def parse(json: String): JsResult[UpdateGlobalVocabularyRequest] = {
    Json.parse(json).validate[UpdateGlobalVocabularyRequest]
  }

  it should "accept an empty object as a general update" in {
    parse("{}").get mustBe UpdateGlobalVocabularyRequest(Seq.empty)
  }

  it should "accept a null iri as a general update" in {
    parse("""{"iri": null}""").get mustBe UpdateGlobalVocabularyRequest(Seq.empty)
  }

  it should "accept a single IRI string" in {
    parse("""{"iri": "http://vocab1"}""").get mustBe UpdateGlobalVocabularyRequest(Seq("http://vocab1"))
  }

  it should "accept an array of IRIs" in {
    parse("""{"iri": ["http://vocab1", "http://vocab2"]}""").get mustBe
      UpdateGlobalVocabularyRequest(Seq("http://vocab1", "http://vocab2"))
  }

  it should "reject an object with other fields but no iri, so misspelled requests do not degrade to a general update" in {
    parse("""{"uri": "http://vocab1"}""") mustBe a[JsError]
  }

  it should "reject a non-string iri" in {
    parse("""{"iri": 42}""") mustBe a[JsError]
  }

  it should "reject a non-object body" in {
    parse(""""http://vocab1"""") mustBe a[JsError]
  }
}
