package org.silkframework.cache

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.util.Uri

/**
 * Base trait for [[EntityCache]] tests. Every implementation is held to the same contract.
 */
trait EntityCacheTest extends AnyFlatSpec with Matchers {

  protected val schema: EntitySchema =
    EntitySchema(typeUri = Uri("urn:Type"), typedPaths = IndexedSeq(UntypedPath("path").asStringTypedPath))

  protected def entity(uri: String): Entity = Entity(uri, IndexedSeq(Seq(uri)), schema)

  /** Creates a cache with the given partition size and blocking disabled, runs the test, then releases its resources. */
  protected def withCache(partitionSize: Int)(test: EntityCache => Unit): Unit

  it should "expose only completed partitions before close and the final partial partition after close" in withCache(2) { cache =>
    // Two full partitions plus a partition that is still being filled (5 entities, partition size 2).
    for (i <- 0 until 5) cache.write(entity(s"urn:e$i"))
    // The partition currently being filled must not be visible yet, otherwise the matcher would read it partially
    // and never revisit the entities added afterwards: 2 completed partitions, not 3.
    cache.partitionCount(0) mustBe 2
    cache.read(0, 0).entities.map(_.uri.toString).toSeq mustBe Seq("urn:e0", "urn:e1")
    cache.read(0, 1).entities.map(_.uri.toString).toSeq mustBe Seq("urn:e2", "urn:e3")
    // Closing seals the partial partition so it becomes visible.
    cache.close()
    cache.partitionCount(0) mustBe 3
    cache.read(0, 2).entities.map(_.uri.toString).toSeq mustBe Seq("urn:e4")
  }

  it should "report no partitions for an empty cache, before and after close" in withCache(2) { cache =>
    cache.partitionCount(0) mustBe 0
    cache.close()
    cache.partitionCount(0) mustBe 0
  }

  it should "return all written entities once closed without losing any" in withCache(2) { cache =>
    for (i <- 0 until 5) cache.write(entity(s"urn:e$i"))
    cache.close()
    cache.readAll.map(_.uri.toString).toSeq mustBe Seq("urn:e0", "urn:e1", "urn:e2", "urn:e3", "urn:e4")
    cache.size mustBe 5
  }
}
