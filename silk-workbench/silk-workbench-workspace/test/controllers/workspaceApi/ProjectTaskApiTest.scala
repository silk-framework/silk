package controllers.workspaceApi

import controllers.workspaceApi.projectTask.RelatedItems
import controllers.workspaceApi.search.ItemType
import helper.IntegrationTestTrait

import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonSerialization
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.{JsValue, Json}
import testWorkspace.Routes
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ProjectTaskApiTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait
    with IntegrationTestTrait
    with Matchers{
  behavior of "Project Task API"

  override def workspaceProviderId: String = "inMemory"

  override def projectPathInClasspath: String = "diProjects/relatedItemsProject.zip"

  override def routes: Option[Class[Routes]] = Some(classOf[testWorkspace.Routes])

  private def relatedItems(taskId: String, textQuery: Option[String] = None): RelatedItems = {
    val path = controllers.projectApi.routes.ProjectTaskApi.relatedItems(projectId, taskId, textQuery).url
    Json.fromJson[RelatedItems](checkResponse(client.url(s"$baseUrl$path").get()).json).get
  }

  private val workflowTask = "workflow"
  private val transformTask = "transform"
  private val customTask = "sparqlSelect"
  private val inputDataset = "dataset"
  private val outputDataset = "outputDataset"

  "Related Items" should "return all directly related items of a project task" in {
    val inDatasetRelatedItems = relatedItems(inputDataset)
    inDatasetRelatedItems.total mustBe 2
    inDatasetRelatedItems.items.map(i => (i.id, i.`type`)) mustBe Seq((transformTask, ItemType.transform.label), (workflowTask, ItemType.workflow.label))
    inDatasetRelatedItems.items.head.itemLinks.size must be >= 1
    val transformRelatedItems = relatedItems(transformTask)
    transformRelatedItems.total mustBe 3
    transformRelatedItems.items.map(_.id).toSet mustBe Set(inputDataset, outputDataset, workflowTask)
  }

  it should "return only related items matching the text query" in {
    // Match in item type
    relatedItems(workflowTask, textQuery = Some(ItemType.task.label)).items.map(_.id) mustBe Seq(customTask)
    // Multi word search in label
    relatedItems(workflowTask, textQuery = Some("spar elect")).items.map(_.id) mustBe Seq(customTask)
    relatedItems(workflowTask, textQuery = Some("spar elect")).total mustBe 4
    // Multi word search in label and type
    relatedItems(workflowTask, textQuery = Some("spar elect " + ItemType.task.label)).items.map(_.id) mustBe Seq(customTask)
  }

  it should "return an auto-configured dataset config" in {
    val inputCsvResource = "needsConfig.csv"
    val csvResource = project.resources.get(inputCsvResource)
    csvResource.writeString(
      """id;label
        |1;test
        |2;test2
        |""".stripMargin
    )
    val path = controllers.projectApi.routes.ProjectTaskApi.postDatasetAutoConfigured(projectId).url
    val initialDataset = DatasetSpec[CsvDataset](CsvDataset(csvResource))
    initialDataset.plugin.separator mustBe ","
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
    val request = client.url(s"$baseUrl$path")
      .addHttpHeaders(ACCEPT -> APPLICATION_JSON)
      .post(JsonSerialization.toJson[GenericDatasetSpec](initialDataset))
    val response = checkResponse(request).json
    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    val autoConfiguredDataset = JsonSerialization.fromJson[GenericDatasetSpec](response).plugin.asInstanceOf[CsvDataset]
    autoConfiguredDataset.separator mustBe ";"
  }
}
