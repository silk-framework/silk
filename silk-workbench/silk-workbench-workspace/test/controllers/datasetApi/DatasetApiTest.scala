package controllers.datasetApi

import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.dataset.{Dataset, DatasetCharacteristics, DatasetSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.plugins.dataset.rdf.datasets.InMemoryDataset
import org.silkframework.serialization.json.JsonHelpers
import play.api.routing.Router
import controllers.datasetApi.payloads.DatasetCharacteristicsPayload._
import org.silkframework.plugins.dataset.csv.CsvDataset

class DatasetApiTest extends AnyFlatSpec with Matchers with IntegrationTestTrait {
  behavior of "Dataset API (v2)"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  val projectId = "testProject"
  private lazy val project = retrieveOrCreateProject(projectId)

  it should "fetch dataset characteristics for a dataset" in {
    val inMemoryDatasetCharacteristics = datasetCharacteristics(InMemoryDataset())
    val csvDatasetCharacteristics = datasetCharacteristics(CsvDataset(project.resources.get("temp.csv")))
    inMemoryDatasetCharacteristics.supportedPathExpressions.languageFilter mustBe true
    csvDatasetCharacteristics.supportedPathExpressions.languageFilter mustBe false
  }

  it should "clear a dataset" in {
    val csvResourceName = "resource.csv"
    val datasetId = "csvDataset"
    val csvResource = project.resources.get(csvResourceName)
    csvResource.writeString("a,b,c\n1,2,3")
    project.resources.exists(csvResourceName) mustBe true
    val csvDataset = new CsvDataset(csvResource)
    project.addTask(datasetId, DatasetSpec(csvDataset))
    val url = controllers.datasetApi.routes.DatasetApi.clearDataset(projectId, datasetId)
    checkResponseExactStatusCode(client.url(s"$baseUrl$url").delete(), NO_CONTENT)
    project.resources.exists(csvResourceName) mustBe false
    csvResource.exists mustBe false
  }

  private var counter = 0

  private def datasetCharacteristics(dataset: Dataset): DatasetCharacteristics = {
    counter += 1
    val datasetId = "dataset" + counter
    project.addTask[GenericDatasetSpec](datasetId, DatasetSpec(dataset))
    val url = controllers.datasetApi.routes.DatasetApi.datasetCharacteristics(projectId, datasetId)
    val response = client.url(s"$baseUrl$url").get()
    val body = checkResponse(response).json
    val characteristics = JsonHelpers.fromJsonValidated[DatasetCharacteristics](body)
    characteristics
  }
}
