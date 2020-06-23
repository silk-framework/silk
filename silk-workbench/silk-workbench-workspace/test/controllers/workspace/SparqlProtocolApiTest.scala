package controllers.workspace

import java.net.URLEncoder

import controllers.sparqlapi.SparqlProtocolApi
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
  private val filmDescriptions = "linkedmdb"
  private val filmPersons = "film_persons"

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
          Json.obj(("f", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/film/350")))),
          Json.obj(("f", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/film/1570"))))
        ))
        ))
      ),
      <sparql xmlns="http://www.w3.org/2005/sparql-results#">
        <head><variable name="f"/></head>
        <results>
          <result><binding name="f"><uri>http://data.linkedmdb.org/resource/film/350</uri></binding></result>
          <result><binding name="f"><uri>http://data.linkedmdb.org/resource/film/1570</uri></binding></result>
        </results>
      </sparql>
    ),
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
      ),
      <sparql xmlns="http://www.w3.org/2005/sparql-results#">
        <head></head>
        <boolean>true</boolean>
      </sparql>),
    "federatedQuery" -> (
      s"""
        |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |PREFIX owl: <http://www.w3.org/2002/07/owl#>
        |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        |PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        |PREFIX mov: <http://data.linkedmdb.org/resource/movie/>
        |PREFIX dct: <http://purl.org/dc/terms/>
        |
        |SELECT ?actor WHERE {
        |  ?f a mov:film.
        |  ?f dct:title ?title.
        |  FILTER(str(?title) = "The Raven")
        |
        |  SERVICE <$baseUrl/workspace/rdfdataset/$projectId/$filmPersons/sparql> {?f mov:actor ?actor. }
        |}
        |ORDER BY ?actor
      """.stripMargin,
      Json.obj(
        ("head", Json.obj(("vars", Json.arr("actor")), ("link", Json.arr()))),
        ("results", Json.obj(("bindings", Json.arr(
          Json.obj(("actor", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/actor/29704")))),
          Json.obj(("actor", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/actor/29956")))),
          Json.obj(("actor", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/actor/29989")))),
          Json.obj(("actor", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/actor/30427")))),
          Json.obj(("actor", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/actor/39109")))),
          Json.obj(("actor", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/actor/61180")))),
          Json.obj(("actor", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/actor/61181")))),
          Json.obj(("actor", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/actor/61182")))),
          Json.obj(("actor", Json.obj(("type", "uri"), ("value", "http://data.linkedmdb.org/resource/actor/61183"))))
        ))
        ))
      ),
      <sparql xmlns="http://www.w3.org/2005/sparql-results#">not used</sparql>)
  )

  for(supportedMediaType <- SparqlProtocolApi.SupportedMediaTyped) {

    "select from a sparql enabled dataset via http get, accepting " + supportedMediaType in {
      val (query, json, xml) = queries("simpleSelect")
      val request = client
        .url(s"$baseUrl/workspace/rdfdataset/$projectId/$filmDescriptions/sparql")
        .addQueryStringParameters(("query", query))
        .addHttpHeaders("Accept" -> supportedMediaType)
      val response = checkResponse(request.get())
      supportedMediaType match{
        case SparqlProtocolApi.SPARQLJSONRESULT => response.json mustBe json
        case SparqlProtocolApi.SPARQLXMLRESULT =>
          response.xml.toString().replaceAll(">\\s+<", "><") mustBe xml.toString().replaceAll(">\\s+<", "><")
      }
    }

    "select from a sparql enabled dataset via http post and plain sparql, accepting " + supportedMediaType in {
      val (query, json, xml) = queries("simpleSelect")
      val request = client
        .url(s"$baseUrl/workspace/rdfdataset/$projectId/$filmDescriptions/sparql")
        .addHttpHeaders("Accept" -> supportedMediaType)
        .addHttpHeaders("Content-Type" -> SparqlProtocolApi.SPARQLQUERY)
      val response = checkResponse(request.post[String](query))
      supportedMediaType match{
        case SparqlProtocolApi.SPARQLJSONRESULT => response.json mustBe json
        case SparqlProtocolApi.SPARQLXMLRESULT =>
          response.xml.toString().replaceAll(">\\s+<", "><") mustBe xml.toString().replaceAll(">\\s+<", "><")
      }
    }

    "select from a sparql enabled dataset via http post and encoded parameters, accepting " + supportedMediaType in {
      val (query, json, xml) = queries("simpleSelect")
      val request = client
        .url(s"$baseUrl/workspace/rdfdataset/$projectId/$filmDescriptions/sparql")
        .addHttpHeaders("Accept" -> supportedMediaType)
        .addHttpHeaders("Content-Type" -> MimeTypes.FORM)
      val encodedQuery = URLEncoder.encode(query, "UTF-8")
      val response = checkResponse(request.post[String]("query=" + encodedQuery))
      supportedMediaType match{
        case SparqlProtocolApi.SPARQLJSONRESULT => response.json mustBe json
        case SparqlProtocolApi.SPARQLXMLRESULT =>
          response.xml.toString().replaceAll(">\\s+<", "><") mustBe xml.toString().replaceAll(">\\s+<", "><")
      }
    }

    "ask a sparql enabled dataset via http get, accepting " + supportedMediaType in {
      val (query, json, xml) = queries("simpleAsk")
      val request = client
        .url(s"$baseUrl/workspace/rdfdataset/$projectId/$filmDescriptions/sparql")
        .addQueryStringParameters(("query", query))
        .addHttpHeaders("Accept" -> supportedMediaType)
      val response = checkResponse(request.get())
      supportedMediaType match{
        case SparqlProtocolApi.SPARQLJSONRESULT => response.json mustBe json
        case SparqlProtocolApi.SPARQLXMLRESULT =>
          response.xml.toString().replaceAll(">\\s+<", "><") mustBe xml.toString().replaceAll(">\\s+<", "><")
      }
    }

    "ask a sparql enabled dataset via http post and plain sparql, accepting " + supportedMediaType in {
      val (query, json, xml) = queries("simpleAsk")
      val request = client
        .url(s"$baseUrl/workspace/rdfdataset/$projectId/$filmDescriptions/sparql")
        .addHttpHeaders("Accept" -> supportedMediaType)
        .addHttpHeaders("Content-Type" -> SparqlProtocolApi.SPARQLQUERY)
      val response = checkResponse(request.post[String](query))
      supportedMediaType match{
        case SparqlProtocolApi.SPARQLJSONRESULT => response.json mustBe json
        case SparqlProtocolApi.SPARQLXMLRESULT =>
          response.xml.toString().replaceAll(">\\s+<", "><") mustBe xml.toString().replaceAll(">\\s+<", "><")
      }
    }

    "ask a sparql enabled dataset via http post and encoded parameters, accepting " + supportedMediaType in {
      val (query, json, xml) = queries("simpleAsk")
      val request = client
        .url(s"$baseUrl/workspace/rdfdataset/$projectId/$filmDescriptions/sparql")
        .addHttpHeaders("Accept" -> supportedMediaType)
        .addHttpHeaders("Content-Type" -> MimeTypes.FORM)
      val encodedQuery = URLEncoder.encode(query, "UTF-8")
      val response = checkResponse(request.post[String]("query=" + encodedQuery))
      supportedMediaType match{
        case SparqlProtocolApi.SPARQLJSONRESULT => response.json mustBe json
        case SparqlProtocolApi.SPARQLXMLRESULT =>
          response.xml.toString().replaceAll(">\\s+<", "><") mustBe xml.toString().replaceAll(">\\s+<", "><")
      }
    }
  }

  "select results from two datasets using a federated query" in {
    val (query, json, _) = queries("federatedQuery")
    val request = client
      .url(s"$baseUrl/workspace/rdfdataset/$projectId/$filmDescriptions/sparql")
      .addHttpHeaders("Accept" -> SparqlProtocolApi.SPARQLJSONRESULT)
      .addQueryStringParameters(("query", query))
    val response = checkResponse(request.get())
    response.json mustBe json
  }

  "should fail for unsupported media type" in {
    val (query, _, _) = queries("simpleSelect")
    val request = client
      .url(s"$baseUrl/workspace/rdfdataset/$projectId/$filmDescriptions/sparql")
      .addHttpHeaders("Accept" -> "text/turtle")
      .addHttpHeaders("Content-Type" -> MimeTypes.FORM)
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val error = intercept[AssertionError](
      checkResponse(request.post[String]("query=" + encodedQuery))
    )
    error.getMessage.contains("text/turtle") mustBe true
  }

  "should fail for unsupported query type" in {
    val query = "DESCRIBE <http://example.org/some/entity>"
    val request = client
      .url(s"$baseUrl/workspace/rdfdataset/$projectId/$filmDescriptions/sparql")
      .addHttpHeaders("Accept" -> SparqlProtocolApi.SPARQLXMLRESULT)
      .addQueryStringParameters(("query", query))
    val error = intercept[AssertionError](
      checkResponse(request.get())
    )
    error.getMessage.contains("Unsupported Query Type") mustBe true
  }
}
