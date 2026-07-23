package controllers.transform

import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.Json
import test.Routes

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Reproduces the shape of the demo "books" mapping to prove that the turtle-output endpoint isolates a
  * single input record even when records share minted object IRIs at several levels.
  *
  * Fixture transform "BookTransform" (input dataset "BookJSON", two book records):
  *   root (schema:Book, uri https://ex.org/book/{isbn})
  *     ├─ author    (schema:Person,       uri https://ex.org/author/{id})
  *     └─ publisher (schema:Organization, uri https://ex.org/publisher/{name})
  *          └─ address (schema:PostalAddress, uri https://ex.org/address/{city})
  *
  * Both books share:
  *  - the SAME author IRI (id "shared"),
  *  - the SAME grandchild address IRI (city "SharedCity") via two different publishers,
  *  - the rdf:type class IRIs (schema:Person/Organization/PostalAddress) inherent to every entity.
  * A naive bidirectional walk hops through any of these shared IRIs into the other record. The endpoint
  * must not: a single record's page contains that record's own subtree only.
  */
class TurtleOutputSharedEntityApiTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with Matchers with IntegrationTestTrait {

  behavior of "Turtle output endpoint (records sharing objects at multiple levels)"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  protected override def routes: Option[Class[Routes]] = Some(classOf[test.Routes])

  override def projectPathInClasspath: String = "controllers/transform/sharedAuthorBooks.zip"

  private val transformTask = "BookTransform"

  private val book1 = "<https://ex.org/book/B1>"
  private val book2 = "<https://ex.org/book/B2>"
  private val sharedAuthor = "<https://ex.org/author/shared>"
  private val sharedAddress = "<https://ex.org/address/SharedCity>"
  private val publisher1 = "<https://ex.org/publisher/P1>"
  private val publisher2 = "<https://ex.org/publisher/P2>"

  private def turtleUrl = s"$baseUrl/transform/tasks/$projectId/$transformTask/turtle"

  private def page(offset: Int, limit: Int) = {
    val url = s"$turtleUrl?offset=$offset&limit=$limit"
    val request = client.url(url).addHttpHeaders("Accept" -> "application/n-triples")
    Await.result(request.post(Json.obj("ruleIds" -> Json.arr("root"))), 100.seconds)
  }

  it should "return the whole tree for both records with a large limit" in {
    val response = page(0, 100)
    response.status mustBe 200
    val body = response.body
    body must include (book1)
    body must include (book2)
    body must include (sharedAuthor)
    body must include (sharedAddress)
    body must include (publisher1)
    body must include (publisher2)
  }

  it should "isolate the first record at limit=1, not bleeding through the shared author, shared address or type hubs" in {
    val body = page(0, 1).body
    // The first book and its own publisher are present ...
    body must include (book1)
    body must include (publisher1)
    // ... the shared author and shared address are part of the first record too ...
    body must include (sharedAuthor)
    body must include (sharedAddress)
    // ... but the second book and its (distinct) publisher must NOT leak in, despite sharing author/address/types.
    body must not include book2
    body must not include publisher2
  }

  it should "isolate the second record at offset=1" in {
    val body = page(1, 1).body
    body must include (book2)
    body must include (publisher2)
    body must include (sharedAuthor)
    body must include (sharedAddress)
    body must not include book1
    body must not include publisher1
  }
}
