package cacheUpdater

import akka.actor.ActorSystem
import helper.IntegrationTestTrait
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.Span
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.plugins.dataset.json.{JsonDataset, JsonSink}
import org.silkframework.rule.input.{TransformInput, Transformer}
import org.silkframework.rule._
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.activity.dataset.TypesCache
import org.silkframework.workspace.activity.transform.TransformPathsCache

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class CacheUpdaterIntegrationTest() extends AnyFlatSpec with IntegrationTestTrait with Matchers with ConfigTestTrait with Eventually with BeforeAndAfterAll {
  behavior of "Cache updater"

  override def workspaceProviderId: String = "inMemory"

  private val beforeJsonContent = """{"id": 1, "sub": {"name": "name1"}}"""
  private val afterJsonContent = """{"id": 1, "sub": {"name": "name1"}, "newSub": {"subName": "name2"}}"""
  override implicit def patienceConfig: PatienceConfig = super.patienceConfig.copy(timeout = Span.convertDurationToSpan(5.seconds))

  override def beforeAll(): Unit = {
    super.beforeAll()
    CacheUpdaterTask.start()(ActorSystem(), ExecutionContext.global)
  }

  it should "update the depending caches when a project resource is updated via a data sink" in {
    val projectId = "triggerProject"
    val p = retrieveOrCreateProject(projectId)
    val resourceName = "resource.json"
    val resource = p.resources.get(resourceName)
    resource.writeString(beforeJsonContent)
    val datasetId = "json2"
    p.addTask(datasetId, DatasetSpec(JsonDataset(resource)))
    val transformId = "transform2"
    val countingTransformer = CountingTransformer(resource)
    val mapping = RootMappingRule(MappingRules(propertyRules = Seq(ComplexMapping(operator = TransformInput(transformer = countingTransformer)))))
    p.addTask(transformId, TransformSpec(DatasetSelection(datasetId), mapping))
    def cachedPaths(): IndexedSeq[String] = p.task[TransformSpec](transformId).activity[TransformPathsCache].value().configuredSchema.typedPaths.map(_.normalizedSerialization)
    def cachedTypes(): Seq[String] = p.task[GenericDatasetSpec](datasetId).activity[TypesCache].value().types
    eventually {
      cachedTypes() mustBe Seq("", "sub")
      cachedPaths() mustBe IndexedSeq("id", "sub", "sub/name")
      countingTransformer.updateCounter.get() mustBe 0
    }
    CacheUpdaterTask.synchronized { // Make sure that there are no updates between calling close and writing the resource
      val jsonSink = new JsonSink(resource)
      jsonSink.close()
      // JSON sink overwrites the resource on close, so we need to update the value afterwards
      resource.writeString(afterJsonContent)
    }
    eventually {
      cachedTypes() mustBe Seq("", "sub", "newSub")
      cachedPaths() mustBe IndexedSeq("id", "sub", "sub/name", "newSub", "newSub/subName")
      countingTransformer.updateCounter.get() mustBe 1
    }
  }

  override def propertyMap: Map[String, Option[String]] = Map(
    CacheUpdaterTask.INTERVAL_CONFIG_KEY -> Some("10ms")
  )
}

/**
 * Counts each update of the provided file resource.
 */
case class CountingTransformer(file: Resource) extends Transformer {

  val updateCounter = new AtomicInteger()

  override def referencedResources: Seq[Resource] = Seq(file)

  override def resourceUpdated(resource: Resource): Unit = {
    updateCounter.incrementAndGet()
  }

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    Seq.empty
  }
}
