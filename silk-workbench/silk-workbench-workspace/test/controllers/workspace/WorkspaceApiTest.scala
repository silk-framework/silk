package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatest.{BeforeAndAfterAll, FlatSpec, MustMatchers}
import org.scalatestplus.play.PlaySpec
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.StringValueType
import org.silkframework.plugins.dataset.rdf.datasets.InMemoryDataset
import org.silkframework.util.Uri
import test.Routes

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
}
