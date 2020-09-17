package controllers.workspace

import controllers.workspace.routes.WorkspaceApi
import controllers.workspace.workspaceApi.TaskLinkInfo
import helper.IntegrationTestTrait
import org.scalatest.{BeforeAndAfterAll, MustMatchers}
import org.scalatestplus.play.PlaySpec
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.StringValueType
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.plugins.dataset.rdf.datasets.InMemoryDataset
import org.silkframework.plugins.dataset.rdf.tasks.SparqlUpdateCustomTask
import org.silkframework.plugins.dataset.xml.XSLTOperator
import org.silkframework.rule.input.{TransformInput, Transformer}
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule._
import org.silkframework.runtime.resource.Resource
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.util.{DPair, Uri}
import play.api.libs.json.Json
import _root_.test.Routes
import org.silkframework.rule.similarity.Comparison

/**
  * Workspace API integration tests.
  */
class WorkspaceApiTest extends PlaySpec with IntegrationTestTrait with MustMatchers with BeforeAndAfterAll {

  private val project = "project"

  override def workspaceProviderId: String = "inMemory"

  protected override def routes: Option[Class[Routes]] = Some(classOf[test.Routes])

  override def beforeAll(): Unit = {
    super.beforeAll()
    retrieveOrCreateProject(project)
  }

  "Project clone endpoint" should {
    "re-create tasks when cloning them" in {
      val inMemoryDataset = InMemoryDataset(clearGraphBeforeExecution = false)
      val tripleSink = inMemoryDataset.tripleSink
      tripleSink.init()
      tripleSink.writeTriple("a", "http://prop", "c", StringValueType())
      tripleSink.close()
      inMemoryDataset.source.retrievePaths("").flatMap(_.propertyUri) mustBe Seq(Uri("http://prop"))
      val datasetName = "oneTripleInMemoryDataset"
      val newProject = "newProject"
      val p = retrieveOrCreateProject(project)
      p.addAnyTask(datasetName, new DatasetSpec(inMemoryDataset))
      checkResponse(client.url(s"$baseUrl/workspace/projects/$project/clone?newProject=$newProject").post(""))
      val clonedInmemoryDataset = retrieveOrCreateProject(newProject).task[GenericDatasetSpec](datasetName)
      clonedInmemoryDataset.data.plugin.asInstanceOf[InMemoryDataset].clearGraphBeforeExecution mustBe false
      // Check that this is a new instance and does not contain the old state
      clonedInmemoryDataset.source.retrievePaths("") mustBe Seq.empty
    }
  }

  private var counter = 0
  private def transformInput(resource: Resource): TransformInput = {
    counter += 1
    TransformInput(s"id$counter", transformer = TestTransformer(Seq(resource)))
  }

  "Resource usage endpoint" should {
    "show resource usages in tasks" in {
      val resourceUsageProject = "resourceUsageProject"
      val resourceName = "resource.file"
      val datasetUsingResource = "datasetUsingResource"
      val taskUsingResource = "taskUsingResource"
      val otherTask = "otherTask"
      val transformTaskUsingResource = "transformTaskUsingResource"
      val linkingTaskUsingResource = "linkingTaskUsingResource"
      val project = retrieveOrCreateProject(resourceUsageProject)
      val resource = project.resources.get(resourceName)
      val differentResource = project.resources.get("differentResource")
      resource.writeString("id\n1")
      val datasetSelection = DatasetSelection("d")
      val transformTask = TransformSpec(
        datasetSelection,
        RootMappingRule(MappingRules(Some(ComplexUriMapping(operator = transformInput(resource)))))
      )
      val linkingTask = LinkSpec(datasetSelection, datasetSelection, LinkageRule(Some(
        Comparison(
          metric = EqualityMetric(),
          inputs = DPair(transformInput(resource), transformInput(resource))
        )
      )))
      project.addAnyTask(transformTaskUsingResource, transformTask)
      project.addAnyTask(linkingTaskUsingResource, linkingTask)
      project.addAnyTask("datasetWithoutResource", new DatasetSpec(InMemoryDataset()))
      project.addAnyTask("datasetWithDifferentResource", new DatasetSpec(CsvDataset(differentResource)))
      project.addAnyTask(datasetUsingResource, new DatasetSpec(CsvDataset(resource)))
      project.addAnyTask(taskUsingResource, XSLTOperator(resource))
      project.addAnyTask(otherTask, SparqlUpdateCustomTask(""))
      val responseJson = checkResponse(createRequest(WorkspaceApi.resourceUsage(project.name, resourceName)).get()).json
      val tasks = Json.fromJson[Seq[TaskLinkInfo]](responseJson).get
      tasks must contain theSameElementsAs Seq(
        TaskLinkInfo(datasetUsingResource, datasetUsingResource, Some(TASK_TYPE_DATASET)),
        TaskLinkInfo(taskUsingResource, taskUsingResource, Some(TASK_TYPE_CUSTOM_TASK)),
        TaskLinkInfo(transformTaskUsingResource, transformTaskUsingResource, Some(TASK_TYPE_TRANSFORM)),
        TaskLinkInfo(linkingTaskUsingResource, linkingTaskUsingResource, Some(TASK_TYPE_LINKING))
      )
    }
  }

  case class TestTransformer(override val referencedResources: Seq[Resource]) extends Transformer {
    override def apply(values: Seq[Seq[String]]): Seq[String] = Seq.empty
  }
}
