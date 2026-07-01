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
  * single input record even when records share a minted object IRI.
  *
  * Fixture transform "BookTransform" (input dataset "BookJSON", two book records):
  *   root (schema:Book, uri https://ex.org/book/{isbn})
  *     ├─ author    (schema:Person,       uri https://ex.org/author/{id})
  *     └─ publisher (schema:Organization, uri https://ex.org/publisher/{name})
  *          └─ address (schema:PostalAddress, uri https://ex.org/address/{city})
  *
  * Both books point at the SAME author IRI (id "shared"), so a naive bidirectional walk would drag the
  * second book (and its publisher/address) into the first book's page. The endpoint must not do that.
  */
class TurtleOutputSharedEntityApiTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with Matchers with IntegrationTestTrait {

  behavior of "Turtle output endpoint (records sharing an object)"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  protected override def routes: Option[Class[Routes]] = Some(classOf[test.Routes])

  override def projectPathInClasspath: String = "controllers/transform/sharedAuthorBooks.zip"

  private val transformTask = "BookTransform"

  private val book1 = "<https://ex.org/book/B1>"
  private val book2 = "<https://ex.org/book/B2>"
  private val sharedAuthor = "<https://ex.org/author/shared>"
  private val publisher1 = "<https://ex.org/publisher/P1>"
  private val publisher2 = "<https://ex.org/publisher/P2>"
  private val address1 = "<https://ex.org/address/C1>"
  private val address2 = "<https://ex.org/address/C2>"

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
    // sanity: both records and the shared author are present when not paginated down
    body must include (book1)
    body must include (book2)
    body must include (sharedAuthor)
    body must include (publisher1)
    body must include (publisher2)
  }

  it should "isolate the first record at limit=1, not bleeding into the record that shares the author" in {
    val body = page(0, 1).body
    // The first book, its publisher and grandchild address are present ...
    body must include (book1)
    body must include (publisher1)
    body must include (address1)
    // ... the shared author is part of the first record too ...
    body must include (sharedAuthor)
    // ... but NOTHING from the second book leaks in, even though it shares the author IRI.
    body must not include book2
    body must not include publisher2
    body must not include address2
  }

  it should "isolate the second record at offset=1" in {
    val body = page(1, 1).body
    body must include (book2)
    body must include (publisher2)
    body must include (address2)
    body must include (sharedAuthor)
    body must not include book1
    body must not include publisher1
    body must not include address1
  }
}
