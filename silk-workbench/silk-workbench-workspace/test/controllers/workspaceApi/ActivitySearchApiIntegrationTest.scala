package controllers.workspaceApi

import controllers.workspaceApi.search.SearchApiModel.{FacetSetting, FacetType, FacetedSearchResult, Facets, KeywordFacetSetting, SortOrder, SortableProperty}
import controllers.workspaceApi.search._
import controllers.workspaceApi.search.activity.ActivitySearchRequest
import controllers.workspaceApi.search.activity.ActivitySearchRequest.{ActivityResult, ActivitySortBy}
import helper.IntegrationTestTrait
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.TransformSpec
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.dataset.TypesCache
import play.api.libs.json._
import testWorkspace.Routes
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ActivitySearchApiIntegrationTest extends AnyFlatSpec
    with SingleProjectWorkspaceProviderTestTrait
    with IntegrationTestTrait
    with Matchers {

  behavior of "Search API"

  override def workspaceProviderId: String = "inMemory"

  override def projectPathInClasspath: String = "diProjects/facetSearchWorkspaceProject.zip"

  override def routes: Option[Class[Routes]] = Some(classOf[testWorkspace.Routes])

  implicit val keywordFacetSettingWrites: Writes[KeywordFacetSetting] = Json.writes[KeywordFacetSetting]
  implicit val facetSettingReader: Writes[FacetSetting] = Json.writes[FacetSetting]
  implicit val activitySearchRequestReader: Writes[ActivitySearchRequest] = Json.writes[ActivitySearchRequest]

  private val transformName = "transformA"
  private val transformLabel = "transform A"

  it should "return all activities for an unrestricted search" in {
    val response = facetedSearchRequest(ActivitySearchRequest(limit = Some(100)))

    val cacheCounts = response.results.groupBy(_.id).view.mapValues(_.size).toMap
    cacheCounts must contain ("GlobalVocabularyCache" -> 1)
    cacheCounts must contain ("TypesCache" -> project.tasks[GenericDatasetSpec].size)
    cacheCounts must contain ("ExecuteTransform" -> project.tasks[TransformSpec].size)

    response.results.find(_.id == "ExecuteTransform").get mustBe
      ActivityResult("ExecuteTransform", "Execute transform", Some(project.id), Some(project.fullLabel),
        Some(transformName), Some(transformLabel), ItemType.transform.id, isCacheActivity = false)
  }

  it should "return all activities of a given parent type" in {
    val globalResponse = facetedSearchRequest(ActivitySearchRequest(itemType = Some(ItemType.global)))
    globalResponse.results.map(_.id) must contain theSameElementsAs Seq("GlobalUriPatternCache", "GlobalVocabularyCache")

    val transformResponse = facetedSearchRequest(ActivitySearchRequest(itemType = Some(ItemType.transform)))
    transformResponse.results.map(_.id) must contain theSameElementsAs Seq("ExecuteTransform", "VocabularyCache", "TransformPathsCache")
  }

  it should "allowing retrieving all cache activities" in {
    val response = facetedSearchRequest(
      ActivitySearchRequest(
        itemType = Some(ItemType.transform),
        facets = Some(Seq(KeywordFacetSetting(FacetType.keyword, Facets.activityType.id, Set("cache")))
    )))
    response.results.map(_.id) must contain theSameElementsAs Seq("VocabularyCache", "TransformPathsCache")
  }

  it should "allowing filtering by text search" in {
    val response = facetedSearchRequest(
      ActivitySearchRequest(
        itemType = Some(ItemType.transform),
        textQuery = Some("vocabulary"),
        facets = Some(Seq(KeywordFacetSetting(FacetType.keyword, Facets.activityType.id, Set("cache")))
      )))
    response.results.map(_.id) must contain theSameElementsAs Seq("VocabularyCache")
  }

  it should "allowing sorting by parent label" in {
    // Per default, results should be sorted by the parent label
    val responseAsc = facetedSearchRequest(
      ActivitySearchRequest(
        itemType = Some(ItemType.dataset)
      ))
    responseAsc.results.map(_.task.get) must contain theSameElementsInOrderAs Seq("csvA", "csvB", "csvC", "jsonXYZ", "output", "xmlA1", "xmlA2")

    // Explicitly sort by label, descending
    val responseDesc = facetedSearchRequest(
      ActivitySearchRequest(
        itemType = Some(ItemType.dataset),
        sortBy = Some(ActivitySortBy.label),
        sortOrder = Some(SortOrder.DESC)
      ))
    responseDesc.results.map(_.task.get) must contain theSameElementsInOrderAs Seq("xmlA2", "xmlA1", "output", "jsonXYZ", "csvC", "csvB", "csvA")
  }

  it should "allowing sorting by recent updates" in {
    // Start two caches after another so they got a defined update order
    val cache1 = project.task[GenericDatasetSpec]("xmlA1").activity[TypesCache]
    cache1.control.waitUntilFinished()
    cache1.startBlocking()
    val cache2 = project.task[GenericDatasetSpec]("xmlA2").activity[TypesCache]
    cache2.control.waitUntilFinished()
    cache2.startBlocking()

    // Most recently updated activities should be returned first
    val response = facetedSearchRequest(
      ActivitySearchRequest(
        itemType = Some(ItemType.dataset),
        textQuery = Some("xMl"),
        sortBy = Some(ActivitySortBy.recentlyUpdated),
        sortOrder = Some(SortOrder.ASC)
      ))
    response.results.map(_.task.get) must contain theSameElementsInOrderAs Seq("xmlA2", "xmlA1")
  }

  private def facetedSearchRequest(facetedSearchRequest: ActivitySearchRequest): ParsedSearchResult = {
    val request = client.url(baseUrl + controllers.workspaceApi.routes.ActivitiesApi.activitySearch().url)
    val response = request.post(Json.toJson(facetedSearchRequest))
    val json = checkResponse(response).json
    val result = Json.fromJson[FacetedSearchResult](json).get
    val parsedResults = result.results.map(Json.fromJson[ActivityResult](_).get)
    ParsedSearchResult(result.total, parsedResults, result.sortByProperties, result.facets)
  }

  case class ParsedSearchResult(total: Int, results: Seq[ActivityResult], sortByProperties: Seq[SortableProperty], facets: Seq[FacetResult])
}
