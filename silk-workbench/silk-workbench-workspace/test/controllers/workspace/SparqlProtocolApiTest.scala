package controllers.workspace

import java.net.URLEncoder

import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.http.MimeTypes
import play.api.libs.json.Json

class SparqlProtocolApiTest extends PlaySpec with IntegrationTestTrait with SingleProjectWorkspaceProviderTestTrait{

  /**
    * Returns the path of the XML zip project that should be loaded before the test suite starts.
    */
  override def projectPathInClasspath: String = "controllers/workspace/sparql_select_test.zip"

  override def workspaceProviderId: String = "mockableInMemoryWorkspace"

  protected override def routes = Some(classOf[test.Routes])

  override val projectId = "sparql_select_test"
  private val rdfDatasetId = "linkedmdb"

  private val queries = Map(
    "simpleSelect" ->     ("""
        |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
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
      """.stripMargin,
      Json.obj(
        ("head", Json.obj(("vars", Json.arr("f")), ("link", Json.arr()))),
        ("results", Json.obj(("bindings", Json.arr(
          Json.obj(("f", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/film/350"))))
        ))
        ))
      )),
    "simpleAsk" -> (
      """
        |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |PREFIX owl: <http://www.w3.org/2002/07/owl#>
        |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        |PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        |PREFIX mov: <http://data.linkedmdb.org/resource/movie/>
        |PREFIX res: <http://data.linkedmdb.org/resource/>
        |
        |ASK WHERE {
        |  ?f a mov:film.
        |  ?f mov:country <http://data.linkedmdb.org/resource/country/AU>.
        |}
      """.stripMargin,
      Json.obj(
        ("head", Json.obj(("vars", Json.arr()), ("link", Json.arr()))),
        ("boolean", true)
      ))
  )


  "select from a sparql enabled dataset via http get" in {
    val (query, expectedResult) = queries("simpleSelect")
    val request = client
      .url(s"$baseUrl/workspace/rdfdataset/$projectId/$rdfDatasetId/select")
      .addQueryStringParameters(("query", query))
      .addHttpHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())
    response.json mustBe expectedResult
  }

  "select from a sparql enabled dataset via http post and plain sparql" in {
    val (query, expectedResult) = queries("simpleSelect")
    val request = client
      .url(s"$baseUrl/workspace/rdfdataset/$projectId/$rdfDatasetId/select")
      .addHttpHeaders("Accept" -> "application/json")
      .addHttpHeaders("Content-Type" -> SparqlProtocolApi.SPARQL)
    val response = checkResponse(request.post[String](query))
    response.json mustBe expectedResult
  }

  "select from a sparql enabled dataset via http post and encoded parameters" in {
    val (query, expectedResult) = queries("simpleSelect")
    val request = client
      .url(s"$baseUrl/workspace/rdfdataset/$projectId/$rdfDatasetId/select")
      .addHttpHeaders("Accept" -> "application/json")
      .addHttpHeaders("Content-Type" -> MimeTypes.FORM)
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val response = checkResponse(request.post[String]("query=" + encodedQuery))
    response.json mustBe expectedResult
  }

  "ask a sparql enabled dataset via http get" in {
    val (query, expectedResult) = queries("simpleAsk")
    val request = client
      .url(s"$baseUrl/workspace/rdfdataset/$projectId/$rdfDatasetId/ask")
      .addQueryStringParameters(("query", query))
      .addHttpHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())
    response.json mustBe expectedResult
  }

  "ask a sparql enabled dataset via http post and plain sparql" in {
    val (query, expectedResult) = queries("simpleAsk")
    val request = client
      .url(s"$baseUrl/workspace/rdfdataset/$projectId/$rdfDatasetId/ask")
      .addHttpHeaders("Accept" -> "application/json")
      .addHttpHeaders("Content-Type" -> SparqlProtocolApi.SPARQL)
    val response = checkResponse(request.post[String](query))
    response.json mustBe expectedResult
  }

  "ask a sparql enabled dataset via http post and encoded parameters" in {
    val (query, expectedResult) = queries("simpleAsk")
    val request = client
      .url(s"$baseUrl/workspace/rdfdataset/$projectId/$rdfDatasetId/ask")
      .addHttpHeaders("Accept" -> "application/json")
      .addHttpHeaders("Content-Type" -> MimeTypes.FORM)
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val response = checkResponse(request.post[String]("query=" + encodedQuery))
    response.json mustBe expectedResult
  }
}
