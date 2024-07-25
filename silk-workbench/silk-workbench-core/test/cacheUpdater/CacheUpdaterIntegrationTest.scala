package cacheUpdater

import helper.IntegrationTestTrait
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.Span
import org.silkframework.dataset.{DatasetSpec, DirtyTrackingFileDataSink}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.plugins.dataset.json.{JsonDataset, JsonSink}
import org.silkframework.rule.{DatasetSelection, TransformSpec}
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.activity.dataset.TypesCache
import org.silkframework.workspace.activity.transform.TransformPathsCache

import scala.concurrent.duration._

class CacheUpdaterIntegrationTest() extends AnyFlatSpec with IntegrationTestTrait with Matchers with ConfigTestTrait with Eventually {
  behavior of "Cache updater"

  override def workspaceProviderId: String = "inMemory"

  private val beforeJsonContent = """{"id": 1, "sub": {"name": "name1"}}"""
  private val afterJsonContent = """{"id": 1, "sub": {"name": "name1"}, "newSub": {"subName": "name2"}}"""
  override implicit def patienceConfig: PatienceConfig = super.patienceConfig.copy(timeout = Span.convertDurationToSpan(5.seconds))

  it should "update the depending caches when a project resource is updated via a data sink" in {
    val projectId = "triggerProject"
    val p = retrieveOrCreateProject(projectId)
    val resourceName = "resource.json"
    val resource = p.resources.get(resourceName)
    resource.writeString(beforeJsonContent)
    val datasetId = "json2"
    p.addTask(datasetId, DatasetSpec(JsonDataset(resource)))
    val transformId = "transform2"
    p.addTask(transformId, TransformSpec(DatasetSelection(datasetId)))
    def cachedPaths(): IndexedSeq[String] = p.task[TransformSpec](transformId).activity[TransformPathsCache].value().configuredSchema.typedPaths.map(_.normalizedSerialization)
    def cachedTypes(): Seq[String] = p.task[GenericDatasetSpec](datasetId).activity[TypesCache].value().types
    eventually {
      cachedTypes() mustBe Seq("", "sub")
      cachedPaths() mustBe IndexedSeq("id", "sub", "sub/name")
    }
    val jsonSink = new JsonSink(resource)
    DirtyTrackingFileDataSink.synchronized {
      jsonSink.close()
      // JSON sink overwrites the resource on close, so we need to update the value afterwards
      resource.writeString(afterJsonContent)
    }
    eventually {
      cachedTypes() mustBe Seq("", "sub", "newSub")
      cachedPaths() mustBe IndexedSeq("id", "sub", "sub/name", "newSub", "newSub/subName")
    }
  }

  override def propertyMap: Map[String, Option[String]] = Map(
    CacheUpdaterTask.INTERVAL_CONFIG_KEY -> Some("10ms")
  )
}
