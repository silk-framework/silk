package controllers.workspaceApi

import controllers.workspaceApi.projectTask.{RelatedItem, RelatedItems}
import controllers.workspaceApi.search.ItemType
import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.Json
import test.Routes

class ProjectTaskApiTest extends FlatSpec with SingleProjectWorkspaceProviderTestTrait
    with IntegrationTestTrait
    with MustMatchers{
  behavior of "Project Task API"

  override def workspaceProviderId: String = "inMemory"

  override def projectPathInClasspath: String = "diProjects/relatedItemsProject.zip"

  override def routes: Option[Class[Routes]] = Some(classOf[test.Routes])

  private def relatedItems(taskId: String, textQuery: Option[String] = None): RelatedItems = {
    val path = controllers.workspaceApi.routes.ProjectTaskApi.relatedItems(projectId, taskId, textQuery).url
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
    inDatasetRelatedItems.items.head.itemLinks.size must be > 2
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
}
