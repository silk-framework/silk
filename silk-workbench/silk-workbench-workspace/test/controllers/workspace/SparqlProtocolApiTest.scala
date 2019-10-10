package controllers.workspace

import java.net.URLEncoder

import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.Json

class SparqlProtocolApiTest extends PlaySpec with IntegrationTestTrait with SingleProjectWorkspaceProviderTestTrait{

  /**
    * Returns the path of the XML zip project that should be loaded before the test suite starts.
    */
  override def projectPathInClasspath: String = "controllers/workspace/sparql_select_test.zip"

  override def workspaceProviderId: String = "mockableInMemoryWorkspace"

  override val projectId = "sparql_select_test"
  private val rdfDatasetId = "linkedmdb"

  "select from a sparql enabled dataset" in {
    val query =
      """
        | PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |PREFIX owl: <http://www.w3.org/2002/07/owl#>
        |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        |PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        |PREFIX mov: <http://data.linkedmdb.org/resource/movie/>
        |PREFIX res: <http://data.linkedmdb.org/resource/>
        |
        |SELECT * WHERE {
        |  ?f a mov:film.
        |  ?f mov:country <http://data.linkedmdb.org/resource/country/AU>.
        |}
      """.stripMargin

    val urlEncodedQuery = URLEncoder.encode(query, "UTF-8")
    val request = client
      .url(s"$baseUrl/workspace/rdfdataset/$projectId/$rdfDatasetId/select?query=$urlEncodedQuery")
      //.addQueryStringParameters(("query", urlEncodedQuery))
      .addHttpHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())
    response.json mustBe Json.obj( )    //TODO
  }
}
