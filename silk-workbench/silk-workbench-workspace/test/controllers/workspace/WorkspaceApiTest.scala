package controllers.workspace

import controllers.workspace.WorkspaceApiTest.TestTransformer
import controllers.workspace.routes.ResourceApi
import controllers.workspace.workspaceApi.TaskLinkInfo
import controllers.workspace.workspaceRequests.CopyTasksResponse
import helper.IntegrationTestTrait
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import org.silkframework.config.MetaData
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.StringValueType
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.plugins.dataset.json.{JsonDataset, JsonSink}
import org.silkframework.plugins.dataset.rdf.datasets.InMemoryDataset
import org.silkframework.plugins.dataset.rdf.tasks.SparqlUpdateCustomTask
import org.silkframework.plugins.dataset.xml.XSLTOperator
import org.silkframework.rule._
import org.silkframework.rule.input.{TransformInput, Transformer}
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.similarity.Comparison
import org.silkframework.runtime.resource.Resource
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.util.{DPair, Uri}
import org.silkframework.workspace.activity.dataset.TypesCache
import org.silkframework.workspace.activity.transform.TransformPathsCache
import play.api.libs.json.Json
import testWorkspace.Routes
import org.scalatest.matchers.must.Matchers
import org.silkframework.runtime.plugin.PluginRegistry

/**
  * Workspace API integration tests.
  */
class WorkspaceApiTest extends PlaySpec with IntegrationTestTrait with Matchers with BeforeAndAfterAll {

  private val project = "project"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  protected override def routes: Option[Class[Routes]] = Some(classOf[testWorkspace.Routes])

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

  "Project copy endpoint" should {
    "copy all tasks in the source project" in {
      val sourceProj = retrieveOrCreateProject("sourceProject")
      val targetProj = retrieveOrCreateProject("targetProject")

      // Add some tasks to the source project
      val datasetName = "dataset"
      val transformName = "transform"

      val resource = sourceProj.resources.get("resource")
      val transformTask = TransformSpec(
        DatasetSelection(datasetName),
        RootMappingRule(MappingRules(Some(ComplexUriMapping(operator = transformInput(resource)))))
      )
      sourceProj.addAnyTask(datasetName, DatasetSpec(InMemoryDataset()))
      sourceProj.addAnyTask(transformName, transformTask)

      // Copy tasks to the target project
      val response = client.url(s"$baseUrl/workspace/projects/${sourceProj.id}/copy")
        .post(Json.parse(
          s""" {
             |    "targetProject": "${targetProj.id}",
             |    "dryRun": false
             |  }
          """.stripMargin
        ))

      // Check response
      val parsedResponse = Json.fromJson[CopyTasksResponse](checkResponse(response).json).get
      parsedResponse.copiedTasks.map(_.id) must contain theSameElementsAs Seq(datasetName, transformName)
      parsedResponse.overwrittenTasks.map(_.id) must contain theSameElementsAs Seq.empty

      // Make sure that tasks have been actually copied
      targetProj.allTasks.map(_.id.toString) must contain theSameElementsAs Seq(datasetName, transformName)
    }
  }

  PluginRegistry.registerPlugin(classOf[TestTransformer])

  private var counter = 0
  private def transformInput(resource: Resource): TransformInput = {
    counter += 1
    TransformInput(s"id$counter", transformer = TestTransformer(resource))
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
      val responseJson = checkResponse(createRequest(ResourceApi.resourceUsage(project.id, resourceName)).get()).json
      val tasks = Json.fromJson[Seq[TaskLinkInfo]](responseJson).get
      tasks must contain theSameElementsAs Seq(
        TaskLinkInfo(datasetUsingResource, MetaData.labelFromId(datasetUsingResource), Some(TASK_TYPE_DATASET)),
        TaskLinkInfo(taskUsingResource, MetaData.labelFromId(taskUsingResource), Some(TASK_TYPE_CUSTOM_TASK)),
        TaskLinkInfo(transformTaskUsingResource, MetaData.labelFromId(transformTaskUsingResource), Some(TASK_TYPE_TRANSFORM)),
        TaskLinkInfo(linkingTaskUsingResource, MetaData.labelFromId(linkingTaskUsingResource), Some(TASK_TYPE_LINKING))
      )
    }
  }

  private val beforeJsonContent = """{"id": 1, "sub": {"name": "name1"}}"""
  private val afterJsonContent = """{"id": 1, "sub": {"name": "name1"}, "newSub": {"subName": "name2"}}"""

  "Resource endpoint" should {
    "trigger updates of depending tasks" in {
      val p = workspaceProject(project)
      val resourceName = "resource.json"
      val resource = p.resources.get(resourceName)
      resource.writeString(beforeJsonContent)
      val datasetId = "json1"
      val jsonDataset = JsonDataset(resource)
      p.addTask(datasetId, DatasetSpec(jsonDataset))
      val transformId = "transform1"
      p.addTask(transformId, TransformSpec(DatasetSelection(datasetId)))
      def cachedPaths(): IndexedSeq[String] = p.task[TransformSpec](transformId).activity[TransformPathsCache].value().configuredSchema.typedPaths.map(_.normalizedSerialization)
      def cachedTypes(): Seq[String] = p.task[GenericDatasetSpec](datasetId).activity[TypesCache].value().types
      eventually {
        cachedTypes() mustBe Seq("", "sub")
        cachedPaths() mustBe IndexedSeq("id", "sub", "sub/name")
      }
      checkResponse(client.url(s"$baseUrl/workspace/projects/$project/resources/$resourceName").put(afterJsonContent))
      eventually {
        cachedTypes() mustBe Seq("", "sub", "newSub")
        cachedPaths() mustBe IndexedSeq("id", "sub", "sub/name", "newSub", "newSub/subName")
      }
    }
  }
}

object WorkspaceApiTest {
  case class TestTransformer(referencedResource: Resource) extends Transformer {
    override def apply(values: Seq[Seq[String]]): Seq[String] = Seq.empty

    override def referencedResources: Seq[Resource] = {
      Seq(referencedResource)
    }
  }
}
