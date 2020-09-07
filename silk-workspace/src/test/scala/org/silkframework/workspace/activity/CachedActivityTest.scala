package org.silkframework.workspace.activity

import java.io.{InputStream, OutputStream}
import java.time.Instant

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.MetaData
import org.silkframework.runtime.activity.{Activity, ActivityContext, TestUserContextTrait, UserContext}
import org.silkframework.runtime.resource.{InMemoryResourceManager, WritableResource}
import org.silkframework.runtime.serialization.ReadContext

import scala.xml.XML

class CachedActivityTest extends FlatSpec with MustMatchers with TestUserContextTrait {

  behavior of "CachedActivity"

  // The value to be loaded and cached
  private var currentValue = ""

  private val initialValue = "Label 1"
  private val updatedValue = "Label 2"

  it must "write the cache whenever the value has been updated" in {
    val cache = new TestCache()
    val cachedActivity = Activity(cache)

    // Make sure that the value will be written initially
    currentValue = initialValue
    CacheResource.writeCount mustBe 0
    cachedActivity.startBlocking()
    CacheResource.writeCount mustBe 1
    cachedActivity.value().label mustBe initialValue
    loadWrittenValue.label mustBe initialValue

    // Make sure that no write happens if the value is not updated
    cachedActivity.startBlocking()
    CacheResource.writeCount mustBe 1
    loadWrittenValue.label mustBe initialValue

    // Make sure that a write happens if the value is updated
    currentValue = updatedValue
    cachedActivity.startBlocking()
    CacheResource.writeCount mustBe 2
    loadWrittenValue.label mustBe updatedValue
  }

  it must "read the cache initially" in {
    val cache = new TestCache()
    val cachedActivity = Activity(cache)

    cachedActivity.value().label must not be updatedValue
    cachedActivity.startBlocking()
    cachedActivity.value().label mustBe updatedValue
  }

  /**
    * Loads the cache value that has been written to the resource.
    */
  private def loadWrittenValue: MetaData = {
    val xml = CacheResource.read(XML.load)
    implicit val readContext: ReadContext = ReadContext()
    MetaData.MetaDataXmlFormat.read(xml)
  }

  /**
    * A cache that writes some meta data for testing.
    * We only consider the label in this test.
    */
  class TestCache() extends CachedActivity[MetaData] {

    override def name: String = "Test cache"

    override def initialValue: Option[MetaData] = Some(MetaData.empty)

    override def loadCache(context: ActivityContext[MetaData])
                          (implicit userContext: UserContext): Unit = {
      if(context.value().label != currentValue) {
        context.value() = MetaData(currentValue)
      }
    }

    override def resource: WritableResource = CacheResource

    override protected val wrappedXmlFormat: WrappedXmlFormat = WrappedXmlFormat()
  }

  /**
    * An in-memory resource that keeps tracks of the number of times it has been written.
    */
  object CacheResource extends WritableResource {

    // The number of times the cache has been written
    var writeCount = 0

    private val resource = InMemoryResourceManager().get("cache")

    override def write(append: Boolean)(write: OutputStream => Unit): Unit = {
      resource.write(append)(write)
      writeCount += 1
    }

    override def delete(): Unit = resource.delete()
    override def name: String = resource.name
    override def path: String = resource.path
    override def exists: Boolean = resource.exists
    override def size: Option[Long] = resource.size
    override def modificationTime: Option[Instant] = resource.modificationTime
    override def inputStream: InputStream = resource.inputStream
  }

}
