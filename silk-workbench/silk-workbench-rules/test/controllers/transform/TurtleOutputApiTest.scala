package controllers.transform

import controllers.transform.transformTask.TurtleOutputService
import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.CustomTask
import org.silkframework.dataset.operations.ClearDatasetOperator
import org.silkframework.rule.{DatasetSelection, ObjectMapping, TransformSpec}
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.Json
import test.Routes

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Tests the "turtle output for selected nodes" endpoint against the hierarchicalPerson fixture.
  *
  * Fixture transform "Transform" (input dataset "Person"):
  *   root
  *     ├─ uri      (subject -> http://domain.com/person/{urlEncode(/ID)})
  *     ├─ direct   (/ID -> rdfs:label)
  *     └─ object   (ObjectMapping over /Properties/Property -> eccemca:property)
  *          ├─ type    (rdf:type eccemca:Property)
  *          └─ direct1 (/Key -> eccemca:key)
  */
class TurtleOutputApiTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with Matchers with IntegrationTestTrait {

  behavior of "Turtle output endpoint"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  protected override def routes: Option[Class[Routes]] = Some(classOf[test.Routes])

  override def projectPathInClasspath: String = "controllers/transform/hierarchicalPerson.zip"

  private val transformTask = "Transform"

  private val labelProperty = "http://www.w3.org/2000/01/rdf-schema#label"
  private val rdfType = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
  private val propertyLink = "https://www.eccemca.com/direct/property"
  private val propertyType = "https://www.eccemca.com/direct/Property"
  private val keyProperty = "https://www.eccemca.com/direct/key"

  private def turtleUrl = s"$baseUrl/transform/tasks/$projectId/$transformTask/turtle"

  private def page(offset: Int, limit: Int, ruleIds: String*) = {
    val url = s"$turtleUrl?offset=$offset&limit=$limit"
    val request = client.url(url).addHttpHeaders("Accept" -> "application/n-triples")
    Await.result(request.post(Json.obj("ruleIds" -> Json.toJson(ruleIds))), 100.seconds)
  }

  // The fixture has two input records; a large limit returns all of them.
  private def selectNTriples(ruleIds: String*) = page(0, 100, ruleIds: _*)

  it should "return only the selected value rule's triples, with the subject IRI but no rdf:type" in {
    val response = selectNTriples("direct")
    response.status mustBe 200
    val body = response.body
    body must include ("<http://domain.com/person/1>")
    body must include (s"<$labelProperty>")
    body must include ("<http://domain.com/person/2>")
    // No triples from the unselected object subtree.
    body must not include rdfType
    body must not include propertyLink
    body must not include keyProperty
  }

  it should "return the whole subtree (type + children) when an object node is selected" in {
    val body = selectNTriples("object").body
    body must include (s"<$propertyLink>")
    body must include (s"<$rdfType>")
    body must include (s"<$propertyType>")
    body must include (s"<$keyProperty>")
    // The sibling label rule was not selected.
    body must not include labelProperty
  }

  it should "emit the connecting object triple and child value, but no rdf:type, for a selected nested value rule" in {
    val body = selectNTriples("direct1").body
    body must include (s"<$keyProperty>")
    // The object link triple is structurally required so the key has a subject.
    body must include (s"<$propertyLink>")
    body must not include rdfType
    body must not include labelProperty
  }

  it should "emit only the rdf:type (and the connecting triple) when just the type rule is selected" in {
    val body = selectNTriples("type").body
    body must include (s"<$rdfType>")
    body must include (s"<$propertyType>")
    body must include (s"<$propertyLink>")
    body must not include keyProperty
  }

  it should "serialize as Turtle by default" in {
    val request = client.url(turtleUrl).addHttpHeaders("Accept" -> "text/turtle")
    val response = Await.result(request.post(Json.obj("ruleIds" -> Json.arr("direct"))), 100.seconds)
    response.status mustBe 200
    response.body must include ("label")
  }

  it should "page by input record: limit=1 returns only the first record" in {
    val body = page(0, 1, "direct").body
    body must include ("<http://domain.com/person/1>")
    body must not include "<http://domain.com/person/2>"
  }

  it should "page by input record: offset=1 returns the second record" in {
    val body = page(1, 1, "direct").body
    body must include ("<http://domain.com/person/2>")
    body must not include "<http://domain.com/person/1>"
  }

  it should "return a complete record (all nested children) within a single page" in {
    // person/1 has three Property children; paging the object with limit=1 returns all three in full.
    val body = page(0, 1, "object").body
    body must include (s"<$propertyType>")
    body must include ("\"1\"")
    body must include ("\"2\"")
    body must include ("\"3\"")
  }

  it should "return an empty page past the last record" in {
    val body = page(5, 1, "direct").body
    body.trim mustBe ""
  }

  it should "return 400 when no rule ids are provided" in {
    val response = Await.result(client.url(turtleUrl).post(Json.obj("ruleIds" -> Json.arr())), 100.seconds)
    response.status mustBe 400
  }

  it should "return 400 for unknown rule ids" in {
    selectNTriples("doesNotExist").status mustBe 400
  }

  it should "return 400 for a negative offset or non-positive limit" in {
    page(-1, 1, "direct").status mustBe 400
    page(0, 0, "direct").status mustBe 400
  }

  it should "return 400 when the limit parameter exceeds the configured maximum" in {
    // Default maximum.
    val response = page(0, TurtleOutputService.defaultMaxLimit + 1, "direct")
    response.status mustBe 400
    (Json.parse(response.body) \ "detail").as[String] must include ("limit")
    // The maximum is configurable.
    ConfigTestTrait.withConfig(TurtleOutputService.maxLimitConfigKey -> Some("2")) {
      page(0, 3, "direct").status mustBe 400
      page(0, 2, "direct").status mustBe 200
    }
  }

  it should "return 413 with a problem-details body when the statement cap is exceeded" in {
    // The 'object' selection materializes ~9 statements for the fixture, so a cap of 3 is exceeded.
    ConfigTestTrait.withConfig(TurtleOutputService.maxStatementsConfigKey -> Some("3")) {
      val response = page(0, 100, "object")
      response.status mustBe 413
      val json = Json.parse(response.body)
      (json \ "title").as[String] mustBe "Turtle debug output too large"
      (json \ "detail").as[String] must include (TurtleOutputService.maxStatementsConfigKey)
    }
    // Untouched config: the same request stays within the default cap.
    page(0, 100, "object").status mustBe 200
  }

  it should "return 400 when the input task of the transformation is not a dataset" in {
    project.addTask[CustomTask]("otherTask", ClearDatasetOperator())
    project.addTask[TransformSpec]("otherInputTransform", TransformSpec(DatasetSelection("otherTask")))
    val url = s"$baseUrl/transform/tasks/$projectId/otherInputTransform/turtle?offset=0&limit=1"
    val response = Await.result(client.url(url).post(Json.obj("ruleIds" -> Json.arr("root"))), 100.seconds)
    response.status mustBe 400
    (Json.parse(response.body) \ "detail").as[String] must include ("not supported as data source")
  }

  it should "follow backward/inverse property mappings when assembling a record's page" in {
    // Clone the fixture transform, but link the Property entities via an inverse mapping:
    // property entity -> eccemca:property -> person (instead of person -> eccemca:property -> property entity).
    val baseSpec = project.task[TransformSpec](transformTask).data
    val root = baseSpec.mappingRule
    val invertedRules = root.rules.copy(propertyRules = root.rules.propertyRules.map {
      case obj: ObjectMapping if obj.id.toString == "object" =>
        obj.copy(target = obj.target.map(_.copy(isBackwardProperty = true)))
      case other => other
    })
    project.addTask[TransformSpec]("BackwardTransform", baseSpec.copy(mappingRule = root.copy(rules = invertedRules)))
    val url = s"$baseUrl/transform/tasks/$projectId/BackwardTransform/turtle?offset=0&limit=1"
    val request = client.url(url).addHttpHeaders("Accept" -> "application/n-triples")
    val response = Await.result(request.post(Json.obj("ruleIds" -> Json.arr("object"))), 100.seconds)
    response.status mustBe 200
    val body = response.body
    // The link triple is inverted: the property entity is the subject, the person the object.
    body must include (s"<$propertyLink> <http://domain.com/person/1>")
    // The linked entities (reached only through the backward link) are part of the record's page.
    body must include (s"<$keyProperty>")
    body must include ("\"1\"")
    body must include ("\"2\"")
    body must include ("\"3\"")
    // The other record does not bleed in.
    body must not include "person/2"
  }
}
