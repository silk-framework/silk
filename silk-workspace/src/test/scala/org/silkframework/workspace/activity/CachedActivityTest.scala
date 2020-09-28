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

  private val initialValue = MetaData("Initial", modified = Some(Instant.ofEpochSecond(0L)))
  private val value1 = MetaData("Label 1", modified = Some(Instant.ofEpochSecond(1L)))
  private val value2 = MetaData("Label 2", modified = Some(Instant.ofEpochSecond(2L)))
  private val value3 = MetaData("Label 3", modified = Some(Instant.ofEpochSecond(2L)))

  // The value to be loaded and cached
  private var currentValue = initialValue

  it must "write the cache whenever the value has been updated" in {
    val cache = new TestCache()
    val cachedActivity = Activity(cache)

    // Make sure that the value will be written initially
    currentValue = value1
    CacheResource.writeCount mustBe 0
    cachedActivity.startBlocking()
    CacheResource.writeCount mustBe 1
    cachedActivity.value().label mustBe value1.label
    loadWrittenValue.label mustBe value1.label

    // Make sure that no write happens if the value is not updated
    cachedActivity.startBlocking()
    CacheResource.writeCount mustBe 1
    loadWrittenValue.label mustBe value1.label

    // Make sure that a write happens if the value is updated
    currentValue = value2
    cachedActivity.startBlocking()
    CacheResource.writeCount mustBe 2
    loadWrittenValue.label mustBe value2.label
  }

  it must "read the cache resource only once" in {
    val cache = new TestCache()
    val cachedActivity = Activity(cache)
    val initialReadCount = CacheResource.readCount

    cachedActivity.startBlocking()
    CacheResource.readCount mustBe (initialReadCount + 1)

    cachedActivity.startBlocking()
    CacheResource.readCount mustBe (initialReadCount + 1)
  }

  it must "reload the cache if the dirty flag is set" in {
    val cache = new TestCache()
    val cachedActivity = Activity(cache)

    // Make sure that the current value is loaded initially
    cache.loadCount = 0
    cachedActivity.value().label must not be value2.label
    cachedActivity.startBlocking()
    cache.loadCount = 1
    cachedActivity.value().label mustBe value2.label

    // Set a new value with the same modified date, so the cache will not update by itself
    currentValue = value3
    cachedActivity.startBlocking()
    cache.loadCount = 2
    cachedActivity.value().label mustBe value2.label

    // Setting dirty forces an update
    cache.startDirty(cachedActivity)
    cachedActivity.waitUntilFinished()
    cache.loadCount = 3
    cachedActivity.value().label mustBe value3.label
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
    * The label will be reloaded if the modified date is newer.
    */
  class TestCache() extends CachedActivity[MetaData] {

    // The number of times loadCache has been called
    var loadCount = 0

    override def name: String = "Test cache"

    override def initialValue: Option[MetaData] = Some(CachedActivityTest.this.initialValue)

    override def loadCache(context: ActivityContext[MetaData], fullReload: Boolean)
                          (implicit userContext: UserContext): Unit = {
      loadCount += 1
      if(currentValue.modified.get.isAfter(context.value().modified.get) || fullReload) {
        context.value() = currentValue
      }
    }

    override def resource: WritableResource = CacheResource

    override protected val wrappedXmlFormat: WrappedXmlFormat = WrappedXmlFormat()
  }

  /**
    * An in-memory resource that keeps tracks of the number of times it has been written.
    */
  object CacheResource extends WritableResource {

    // The number of times the cache has been read
    var readCount = 0

    // The number of times the cache has been written
    var writeCount = 0

    private val resource = InMemoryResourceManager().get("cache")

    override def inputStream: InputStream = {
      readCount += 1
      resource.inputStream
    }

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
  }

}
