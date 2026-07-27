package controllers.transform

import helper.IntegrationTestTrait
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import test.Routes

/**
  * Covers the peak endpoints' SparqlSelectCustomTask input path (`PeakTransformApi.peakIntoSparqlSelectTask`),
  * which the plain PeakTransformApiTest never exercises: there the transform's input is always a Dataset task.
  * Here the transform's input is a SPARQL Select custom task backed by an RDF in-memory dataset, and the
  * pagination query parameters (limit/offset/maxTryEntities/search/includeTotal) are asserted the same way as
  * for the Dataset-backed path, including that `maxTryEntities` caps the number of rows actually fetched from
  * the underlying SPARQL query (not just the result page).
  */
class PeakTransformApiSparqlSelectTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with Matchers with IntegrationTestTrait {

  behavior of "Peak API (SPARQL select input)"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  protected override def routes: Option[Class[Routes]] = Some(classOf[test.Routes])

  private val transformTask = "transform_sparql"
  private val ruleId = "name"

  it should "return sample values from a SPARQL select input dataset without pagination metadata by default" in {
    val peakResult = peakRequest()
    peakResult.status.id mustBe "success"
    peakResult.sourcePaths mustBe Some(Seq(Seq("/name")))
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("Alice")), Seq("Alice")),
      PeakResult(Seq(Seq("Bob")), Seq("Bob")),
      PeakResult(Seq(Seq("Carol")), Seq("Carol"))
    ))
    peakResult.total mustBe None
    peakResult.totalIsExact mustBe None
    peakResult.nextOffset mustBe None
  }

  it should "paginate the SPARQL select preview via offset/limit" in {
    val peakResult = peakRequest(limit = Some(2), offset = Some(2))
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("Carol")), Seq("Carol")),
      PeakResult(Seq(Seq("Dave")), Seq("Dave"))
    ))
    // 5 entities in total; offset=2 implies includeTotal, and the whole (small) budget is scanned exactly.
    peakResult.total mustBe Some(5)
    peakResult.totalIsExact mustBe Some(true)
    peakResult.nextOffset mustBe Some(4)
  }

  it should "cap the number of rows fetched from the underlying SPARQL query via maxTryEntities" in {
    // sourceFetchSize = offset(0) + maxTryEntities(2) = 2, so only the first 2 entities (Alice, Bob) are
    // ever fetched from the SPARQL query - Carol/Dave/Eve are never even retrieved.
    val peakResult = peakRequest(limit = Some(1), maxTryEntities = Some(2), includeTotal = Some(true))
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("Alice")), Seq("Alice"))
    ))
    // Alice filled the page, Bob is the one entity in the tail -> total = 1 (page) + 1 (tail) = 2.
    peakResult.total mustBe Some(2)
    // The scan hit the maxTryEntities cap exactly (2 tried == sourceFetchSize 2), so more entities may
    // exist beyond the budget - the total is not guaranteed exact.
    peakResult.totalIsExact mustBe Some(false)
    peakResult.nextOffset mustBe Some(1)
  }

  it should "filter the SPARQL select preview by a case-insensitive search substring" in {
    // Of Alice/Bob/Carol/Dave/Eve, only Bob and Carol contain a (lower-cased) 'o'.
    val peakResult = peakRequest(limit = Some(10), search = Some("o"))
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("Bob")), Seq("Bob")),
      PeakResult(Seq(Seq("Carol")), Seq("Carol"))
    ))
    peakResult.total mustBe Some(2)
    peakResult.totalIsExact mustBe Some(true)
  }

  private def peakRequest(limit: Option[Int] = None,
                          maxTryEntities: Option[Int] = None,
                          offset: Option[Int] = None,
                          search: Option[String] = None,
                          includeTotal: Option[Boolean] = None): PeakResults = {
    val peakUrl = controllers.transform.routes.PeakTransformApi.peak(
      projectId, transformTask, ruleId,
      limit = limit.getOrElse(PeakTransformApi.TRANSFORMATION_PREVIEW_LIMIT),
      maxTryEntities = maxTryEntities.getOrElse(PeakTransformApi.MAX_TRY_ENTITIES_DEFAULT),
      offset = offset.getOrElse(0),
      search = search,
      includeTotal = includeTotal.getOrElse(false)
    ).url
    val jsonResponse = checkResponse(client.url(s"$baseUrl$peakUrl").post("")).json
    JsonHelpers.fromJsonValidated[PeakResults](jsonResponse)
  }

  /**
    * Returns the path of the XML zip project that should be loaded before the test suite starts.
    * Contains an RDF in-memory dataset ('rdfData', five `ex:name` triples), a SPARQL Select custom task
    * ('sparqlSelectOp', its 'Optional SPARQL dataset' pointing at 'rdfData'), and a transform task
    * ('transform_sparql') whose input is that custom task, with a single direct value rule ('name') mapping
    * the SPARQL result's '/name' column.
    */
  override def projectPathInClasspath: String = "controllers/transform/peakSparqlSelect.zip"
}
