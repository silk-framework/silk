package controllers.workspace

import controllers.workspaceApi.search.SearchApiModel.{FacetSetting, FacetType, FacetedSearchRequest, FacetedSearchResult,
  Facets, ItemType, KeywordFacetSetting, SortBy, SortOrder, SortableProperty}
import controllers.workspaceApi.search._
import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.MetaData
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{SingleProjectWorkspaceProviderTestTrait, WorkspaceFactory}
import play.api.libs.json._
import test.Routes

class SearchApiIntegrationTest extends FlatSpec
    with SingleProjectWorkspaceProviderTestTrait
    with IntegrationTestTrait
    with MustMatchers{
  behavior of "Search API"

  override def workspaceProviderId: String = "inMemory"

  override def projectPathInClasspath: String = "diProjects/facetSearchWorkspaceProject.zip"

  override def routes: Option[Class[Routes]] = Some(classOf[test.Routes])

  lazy implicit val facetSettingWrites: Writes[FacetSetting] = Json.writes[FacetSetting]
  lazy implicit val keywordFacetSettingWrites: Writes[KeywordFacetSetting] = Json.writes[KeywordFacetSetting]
  lazy implicit val facetSearchRequestWrites: Writes[FacetedSearchRequest] = Json.writes[FacetedSearchRequest]
  lazy implicit val sortablePropertyFormat: Format[SortableProperty] = Json.format[SortableProperty]
  lazy implicit val keywordFacetValueReads: Reads[KeywordFacetValue] = Json.reads[KeywordFacetValue]
  lazy implicit val facetResultReads: Reads[FacetResult] = new Reads[FacetResult] {
    override def reads(json: JsValue): JsResult[FacetResult] = {
      val facetType = (json \ SearchApiModel.TYPE).get.as[String]
      assert(FacetType.facetTypeSet.contains(facetType), "Facet type invalid!")
      val facetValues = FacetType.withName(facetType) match {
        case FacetType.keyword =>
          (json \ SearchApiModel.VALUES).as[JsArray].value.map(keywordFacetValueReads.reads(_).get)
      }
      JsSuccess(FacetResult(
        id = (json \ SearchApiModel.ID).as[String],
        label = (json \ SearchApiModel.LABEL).as[String],
        description = (json \ SearchApiModel.DESCRIPTION).as[String],
        `type` = facetType,
        values = facetValues
      ))
    }
  }
  lazy implicit val facetValuesReads: Reads[FacetValue] = Json.reads[FacetValue]
  lazy implicit val facetedSearchResultReads: Reads[FacetedSearchResult] = Json.reads[FacetedSearchResult]

  private def facetedSearchRequest(facetedSearchRequest: FacetedSearchRequest): (FacetedSearchResult, JsValue) = {
    val response = client.url(s"$baseUrl/api/workspace/searchItems").post(Json.toJson(facetedSearchRequest))
    val json = checkResponse(response).json
    Json.fromJson[FacetedSearchResult](json) match {
      case JsError(errors) => throw new RuntimeException("JSON could not be parsed: " + errors)
      case JsSuccess(value, _) => (value, json)
    }
  }

  it should "return item types" in {
    val response = client.url(s"$baseUrl/api/workspace/searchConfig/types").get()
    val json = checkResponse(response).json
    (json \ "label").asOpt[String] mustBe Some("Type")
    val typeIds = (json \ "values").as[JsArray].value.map(v => (v \ "id").as[String])
    typeIds mustBe Seq("project", "workflow", "dataset", "transform", "linking", "task")
  }

  it should "return all project task item types if a project ID is defined" in {
    val response = client.url(s"$baseUrl/api/workspace/searchConfig/types?projectId=proj").get()
    val json = checkResponse(response).json
    val typeIds = (json \ "values").as[JsArray].value.map(v => (v \ "id").as[String])
    typeIds mustBe Seq("workflow", "dataset", "transform", "linking", "task")
  }

  private val allDatasets = Seq("csvA", "csvB", "csvC", "jsonXYZ", "output", "xmlA1", "xmlA2")
  private val allResults = Seq("singleProject", "csvA", "csvB", "csvC", "jsonXYZ", "output", "xmlA1", "xmlA2", "transformA")

  it should "return all tasks (pages) for a unrestricted search" in {
    val (response, _) = facetedSearchRequest(FacetedSearchRequest())
    resultItemIds(response) mustBe allResults
  }

  it should "only return task results for project restricted searches" in {
    val (response, _) = facetedSearchRequest(FacetedSearchRequest(project = Some(projectId)))
    resultItemIds(response) mustBe allResults.drop(1)
  }

  it should "only return task results for project restricted searches with text query" in {
    val (response, _) = facetedSearchRequest(FacetedSearchRequest(project = Some(projectId), textQuery = Some("o    s")))
    resultItemIds(response) mustBe Seq("jsonXYZ", "transformA")
  }

  it should "page through the results correctly" in {
    var results = allResults.toList
    var offset = 0
    val pageSize = 3
    while(results.nonEmpty) {
      val expectedResults = results.take(3)
      results = results.drop(3)
      val (response, _) = facetedSearchRequest(FacetedSearchRequest(offset = Some(offset), limit = Some(pageSize)))
      resultItemIds(response) mustBe expectedResults
      offset += pageSize
    }
  }

  it should "return only generic facets for an unrestricted search" in {
    val (facetedSearchResult, _) = facetedSearchRequest(FacetedSearchRequest())
    facetedSearchResult.facets.map(_.id) mustBe Seq(Facets.createdBy.id, Facets.lastModifiedBy.id)
  }

  private val CSV = "csv"

  it should "return the right facet counts and result for a dataset type restricted search" in {
    val (facetedSearchResult, json) = facetedSearchRequest(FacetedSearchRequest(itemType = Some(ItemType.dataset)))
    val firstFacet = (json \ "facets").as[JsArray].value.head
    (firstFacet \ "values").as[JsArray].value.head.asInstanceOf[JsObject].keys mustBe Set("id", "label", "count")
    checkAndGetDatasetFacetValues(facetedSearchResult) mustBe Seq(
      Seq((CSV,3), ("xml",2), ("inMemory",1), ("json",1)),
      Seq(("a.xml",2), ("a.csv",1), ("b.csv",1), ("c.csv",1), ("xyz.json",1)),
      Seq(("", 7)), // Unknown user
      Seq(("", 7))  // Unknown user
    )
    resultItemIds(facetedSearchResult) mustBe allDatasets
  }

  // Returns all item IDs of the search results
  private def resultItemIds(response: FacetedSearchResult): Seq[String] = {
    response.results.map(result => (result \ "id").as[String])
  }

  it should "return the right facet counts and results when restricting the search via dataset type facet" in {
    val (facetedSearchResult, _) = facetedSearchRequest(
      FacetedSearchRequest(itemType = Some(ItemType.dataset), facets = Some(Seq(
        KeywordFacetSetting(FacetType.keyword, Facets.datasetType.id, Set(CSV))
      ))))
    checkAndGetDatasetFacetValues(facetedSearchResult) mustBe Seq(
      Seq((CSV,3), ("xml",2), ("inMemory",1), ("json",1)),
      Seq(("a.csv",1), ("b.csv",1), ("c.csv",1)),
      Seq(("", 3)),
      Seq(("", 3))
    )
    resultItemIds(facetedSearchResult) mustBe Seq("csvA", "csvB", "csvC")
  }

  it should "return the right facet counts and results when restricting the search via dataset type and resource facet" in {
    val (facetedSearchResult, _) = facetedSearchRequest(
      FacetedSearchRequest(itemType = Some(ItemType.dataset), facets = Some(Seq(
        KeywordFacetSetting(FacetType.keyword, Facets.datasetType.id, Set(CSV)),
        KeywordFacetSetting(FacetType.keyword, Facets.fileResource.id, Set("a.csv", "b.csv"))
      ))))
    checkAndGetDatasetFacetValues(facetedSearchResult) mustBe Seq(
      Seq((CSV,2)),
      Seq(("a.csv",1), ("b.csv",1), ("c.csv",1)),
      Seq(("", 2)),
      Seq(("", 2))
    )
    resultItemIds(facetedSearchResult) mustBe Seq("csvA", "csvB")
  }

  it should "sort the results by label/ID" in {
    resultItemIds(facetedSearchRequest(
      FacetedSearchRequest(itemType = Some(ItemType.dataset), sortBy = Some(SortBy.label), sortOrder = Some(SortOrder.DESC))
    )._1) mustBe Seq("xmlA2", "xmlA1", "output", "jsonXYZ", "csvC", "csvB", "csvA")
    resultItemIds(facetedSearchRequest(
      FacetedSearchRequest(itemType = Some(ItemType.dataset), sortBy = Some(SortBy.label), sortOrder = Some(SortOrder.ASC))
    )._1) mustBe Seq("csvA", "csvB", "csvC", "jsonXYZ", "output", "xmlA1", "xmlA2")
    resultItemIds(facetedSearchRequest( // Default sort order is ascending
      FacetedSearchRequest(itemType = Some(ItemType.dataset), sortBy = Some(SortBy.label))
    )._1) mustBe Seq("csvA", "csvB", "csvC", "jsonXYZ", "output", "xmlA1", "xmlA2")
  }

  it should "filter by (multi-word) text query" in {
    resultItemIds(facetedSearchRequest(
      FacetedSearchRequest(sortBy = Some(SortBy.label), textQuery = Some("ml"))
    )._1) mustBe Seq("xmlA1", "xmlA2")
    resultItemIds(facetedSearchRequest(
      FacetedSearchRequest(sortBy = Some(SortBy.label), textQuery = Some("ou ut"))
    )._1) mustBe Seq("output")
    resultItemIds(facetedSearchRequest(
      FacetedSearchRequest(sortBy = Some(SortBy.label), textQuery = Some("js xyZ"))
    )._1) mustBe Seq("jsonXYZ")
  }

  it should "search for project resources" in {
    val resourcesManager = project.resources
    val expectedNames = for(i <- 0 to 9) yield {
      val name = s"Res$i" + (if(i%2 == 0) "even" else "odd")
      resourcesManager.get(name).writeString("a" * i)
      name
    }
    // default search
    val defaultResults = resourceSearch(ResourceSearchRequest())
    resourceNames(defaultResults) mustBe expectedNames.take(ResourceSearchRequest.DEFAULT_LIMIT)
    // limit and offset search
    val smallPageResult = resourceSearch(ResourceSearchRequest(limit = Some(3), offset = Some(2)))
    val expectedSmallNames = expectedNames.slice(2, 5)
    resourceNames(smallPageResult) mustBe expectedSmallNames
    smallPageResult.flatMap(_.get(ResourceSearchRequest.SIZE_PARAM)).map(_.as[Int]) mustBe Seq(2, 3, 4)
    // text search
    resourceNames(resourceSearch(ResourceSearchRequest(limit = Some(2), offset = Some(2), searchText = Some("res Even")))) mustBe
      expectedNames.zipWithIndex.filter(_._2 % 2 == 0).slice(2, 4).map(_._1)
  }

  it should "find a project and task by their descriptions" in {
    val uniqueId = "veryUnique"
    val newProject = "newProject"
    val newTask = "newTask"
    val metaData = MetaData("", Some("A" + uniqueId + "Z"))

    val project = retrieveOrCreateProject(newProject)
    try {
      WorkspaceFactory().workspace.updateProjectMetaData(newProject, metaData)
      val workflow = Workflow(Seq.empty, Seq.empty)
      project.addAnyTask(newTask, workflow, metaData)
      resultItemIds(facetedSearchRequest(
        FacetedSearchRequest(itemType = Some(ItemType.workflow), textQuery = Some(uniqueId))
      )._1) mustBe Seq(newTask)
      resultItemIds(facetedSearchRequest(
        FacetedSearchRequest(itemType = Some(ItemType.project), textQuery = Some(uniqueId))
      )._1) mustBe Seq(newProject)
    } finally {
      WorkspaceFactory().workspace.removeProject(newProject)
    }
  }

  private def resourceNames(defaultResults: IndexedSeq[collection.Map[String, JsValue]]) = {
    defaultResults.flatMap(_.get(ResourceSearchRequest.NAME_PARAM)).map(_.as[String])
  }

  private lazy val resourceSearchUrl = controllers.workspace.routes.WorkspaceApi.getResources(projectId)
  private def resourceSearch(request: ResourceSearchRequest): IndexedSeq[collection.Map[String, JsValue]] = {
    val result = checkResponse(client.url(s"$baseUrl$resourceSearchUrl?${request.queryString}").get()).json
    result.as[JsArray].value.map(_.asInstanceOf[JsObject].value)
  }

  private def checkAndGetDatasetFacetValues(response: FacetedSearchResult): Seq[Seq[(String, Int)]] = {
    response.facets.size mustBe 4
    response.facets.map(_.id) mustBe Seq(Facets.datasetType.id, Facets.fileResource.id, Facets.createdBy.id, Facets.lastModifiedBy.id)
    response.facets.map(extractKeyWordsWithCounts)
  }

  private def extractKeyWordsWithCounts(facetResult: FacetResult): Seq[(String, Int)] = {
    facetResult.`type` mustBe "keyword"
    facetResult.values.
        map(_.asInstanceOf[KeywordFacetValue]).
        map(k => (k.id, k.count.get))
  }
}
