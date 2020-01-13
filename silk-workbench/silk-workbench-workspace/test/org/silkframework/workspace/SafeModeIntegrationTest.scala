package org.silkframework.workspace

import helper.IntegrationTestTrait
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, MustMatchers}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.SafeModeException
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.runtime.activity.Status.Idle
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.{ConfigTestTrait, SparqlMockServerTrait}
import org.silkframework.workspace.activity.transform.TransformPathsCache

class SafeModeIntegrationTest extends FlatSpec
    with ConfigTestTrait
    with SparqlMockServerTrait
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with SingleProjectWorkspaceProviderTestTrait
    with IntegrationTestTrait
    with MustMatchers {

  behavior of "safe production mode"

  private val inputSparqlDataset = "inputSPARQL"
  private val sparqlWorkflow = "sparqlWorkflow"
  private val output = "outputCSV"
  private val csvWorkflow = "csvWorkflow"
  private val sparqlTransform = "transformSparql"
  private val customerTransform = "transformCustomers"
  private var sparqlServerPort = -1
  private val sparqlDatasetEndpointParameter = "endpointURI"

  override def workspaceProviderId: String = "inMemory"
  protected override def routes = Some(classOf[test.Routes])

  override def propertyMap: Map[String, Option[String]] = Map(
    "config.production.safeMode" -> Some("true"),
    "caches.config.enableAutoRun" -> Some("false")
  )

  override def projectPathInClasspath: String = "diProjects/safeModeProject.zip"

  override def beforeAll(): Unit = {
    super.beforeAll()
    sparqlServerPort = startServer(Seq(sparqlContent(
      variables = Seq("prop"),
      values = Seq(Seq("val1"))
    )))
  }

  override def afterAll(): Unit = {
    stopAllRegisteredServers()
    super.afterAll()
  }

  private var sparqlDatasetInitialized = false

  override def beforeEach(): Unit = {
    super.beforeEach()
    if(!sparqlDatasetInitialized) {
      implicit val prefixes: Prefixes = Prefixes.empty
      implicit val resourceManager: ResourceManager = project.resources
      val task = project.task[GenericDatasetSpec](inputSparqlDataset)
      val oldTask = task.data
      val oldProperties = oldTask.properties
      val newProperties = oldProperties.filter(prop => prop._1 != sparqlDatasetEndpointParameter && prop._1 != "type") ++
          Seq(sparqlDatasetEndpointParameter -> s"http://localhost:$sparqlServerPort/sparql")
      task.update(oldTask.withProperties(newProperties.toMap))
      sparqlDatasetInitialized = true
    }
    outputDataset.entitySink.clear()
    checkOutputEmpty()
  }

  private def outputDataset: GenericDatasetSpec = project.task[GenericDatasetSpec](output).data

  private def checkOutputEmpty(): Unit = {
    if(project.resources.get("output.csv").exists) {
      outputDataset.source.retrievePaths("").size mustBe 0
    }
  }

  private def transformation(id: String): ProjectTask[TransformSpec] = project.task[TransformSpec](id)

  private def checkOutputPresent(): Unit = {
    outputDataset.source.retrievePaths("").size must be > 0
  }

  it should "not execute any caches automatically" in {
    val idle = Idle()
    for(projectTask <- project.allTasks;
        activity <- projectTask.activities) {
      activity.status.get.foreach { status =>
        assert(status == idle, s"Activity of ${projectTask.id} has not been idle: ${activity.activityType.getSimpleName}")
      }
    }
  }

  it should "prevent access to external datasets when safe-mode is on" in {
    val activity = transformation(sparqlTransform).activity[ExecuteTransform]
    intercept[SafeModeException] {
      activity.control.startBlocking()
    }
    checkOutputEmpty()
    activity.status().failed mustBe true
    intercept[SafeModeException] {
      transformation(sparqlTransform).activity[TransformPathsCache].control.startBlocking()
    }
  }

  it should "allow access to local datasets when safe-mode is on" in {
    transformation(customerTransform).activity[ExecuteTransform].control.startBlocking()
    checkOutputPresent()
    executeWorkflow(csvWorkflow)
    checkOutputPresent()
  }

  it should "allow access to external and (local) datasets when safe-mode is on in the context of workflows" in {
    executeWorkflow(sparqlWorkflow)
    checkOutputPresent()
    executeWorkflow(csvWorkflow)
    checkOutputPresent()
  }

  it should "allow access to external datasets when safe-mode is off" in {
    val response = client.url(s"$baseUrl/core/safeMode?enable=false").post("")
    checkResponse(response)
    executeTransformTask(projectId, sparqlTransform)
    checkOutputPresent()
  }
}
