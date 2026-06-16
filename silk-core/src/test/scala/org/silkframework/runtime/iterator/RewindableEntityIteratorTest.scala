package org.silkframework.runtime.iterator

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.entity.{Entity, EntitySchema, Restriction}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.util.Uri

class RewindableEntityIteratorTest extends AnyFlatSpec with Matchers {

  private val schema: EntitySchema =
    EntitySchema(Uri(""), IndexedSeq(UntypedPath("p1").asStringTypedPath), Restriction.empty)

  private def entity(n: Int): Entity =
    Entity(s"http://example.com/e$n", IndexedSeq(Seq(s"val$n")), schema)

  private def iteratorOf(entities: Entity*): CloseableIterator[Entity] =
    CloseableIterator(entities.iterator)

  // --- RewindableEntityIterator.load() ---

  behavior of "RewindableEntityIterator.load()"

  it should "return the same instance if input is already a RewindableEntityIterator" in {
    val existing = new InMemoryRewindableEntityIterator(Iterable(entity(1)))
    val result = RewindableEntityIterator.load(existing, schema)
    result eq existing shouldBe true
  }

  it should "return an InMemoryRewindableEntityIterator for an empty input" in {
    val result = RewindableEntityIterator.load(iteratorOf(), schema)
    result shouldBe a[InMemoryRewindableEntityIterator]
    result.hasNext shouldBe false
  }

  it should "return an InMemoryRewindableEntityIterator for a single-entity input" in {
    val e = entity(1)
    val result = RewindableEntityIterator.load(iteratorOf(e), schema)
    result shouldBe a[InMemoryRewindableEntityIterator]
    result.toSeq shouldBe Seq(e)
  }

  it should "return a FileRewindableEntityIterator for a multi-entity input" in {
    val entities = Seq(entity(1), entity(2), entity(3))
    val result = RewindableEntityIterator.load(iteratorOf(entities: _*), schema)
    result shouldBe a[FileRewindableEntityIterator]
    result.toSeq shouldBe entities
    result.close()
  }

  it should "close the original iterator after load()" in {
    var closed = false
    val inner = iteratorOf(entity(1), entity(2), entity(3))
    val tracked = new CloseableIterator[Entity] {
      override def hasNext: Boolean = inner.hasNext
      override def next(): Entity = inner.next()
      override def close(): Unit = { closed = true; inner.close() }
    }
    val result = RewindableEntityIterator.load(tracked, schema)
    closed shouldBe true
    result.close()
  }

  // --- InMemoryRewindableEntityIterator ---

  behavior of "InMemoryRewindableEntityIterator"

  it should "report hasNext = false and return an empty newIterator() for empty entities" in {
    val iter = new InMemoryRewindableEntityIterator(Iterable.empty)
    iter.hasNext shouldBe false
    iter.newIterator().hasNext shouldBe false
  }

  it should "iterate to a single entity and allow rewinding" in {
    val e = entity(1)
    val iter = new InMemoryRewindableEntityIterator(Iterable(e))
    iter.next() shouldBe e
    iter.hasNext shouldBe false
    val rewound = iter.newIterator()
    rewound.next() shouldBe e
    rewound.hasNext shouldBe false
  }

  it should "iterate multiple entities correctly and rewind" in {
    val entities = Seq(entity(1), entity(2), entity(3))
    val iter = new InMemoryRewindableEntityIterator(entities)
    iter.toSeq shouldBe entities
    iter.newIterator().toSeq shouldBe entities
  }

  it should "provide independent iterators from multiple newIterator() calls" in {
    val entities = Seq(entity(1), entity(2))
    val iter = new InMemoryRewindableEntityIterator(entities)
    val it1 = iter.newIterator()
    val it2 = iter.newIterator()
    it1.next() // consume first from it1
    it2.toSeq shouldBe entities // it2 should still yield all
    it1.toSeq shouldBe Seq(entity(2)) // it1 should yield remaining
  }

  // --- FileRewindableEntityIterator ---

  behavior of "FileRewindableEntityIterator"

  it should "round-trip entities correctly" in {
    val entities = Seq(entity(1), entity(2), entity(3))
    val iter = FileRewindableEntityIterator.load(iteratorOf(entities: _*), schema)
    iter.toSeq shouldBe entities
    iter.close()
  }

  it should "rewind to the start via newIterator()" in {
    val entities = Seq(entity(1), entity(2), entity(3))
    val iter = FileRewindableEntityIterator.load(iteratorOf(entities: _*), schema)
    iter.toSeq // consume
    val rewound = iter.newIterator()
    rewound.toSeq shouldBe entities
    rewound.close()
    iter.close()
  }

  it should "provide independent iterators from multiple newIterator() calls" in {
    val entities = Seq(entity(1), entity(2), entity(3))
    val iter = FileRewindableEntityIterator.load(iteratorOf(entities: _*), schema)
    val it1 = iter.newIterator()
    val it2 = iter.newIterator()
    it1.toSeq shouldBe entities
    it2.toSeq shouldBe entities
    it1.close()
    it2.close()
    iter.close()
  }

  it should "delete the temp file after the last instance is closed" in {
    val entities = Seq(entity(1), entity(2))
    val iter = FileRewindableEntityIterator.load(iteratorOf(entities: _*), schema)
    val it2 = iter.newIterator().asInstanceOf[FileRewindableEntityIterator]

    // Obtain the file reference before closing
    val fileField = iter.getClass.getDeclaredField("file")
    fileField.setAccessible(true)
    val holder = fileField.get(iter).asInstanceOf[FileRewindableEntityIterator.TempFileHolder]
    val tempFile = holder()

    tempFile.exists() shouldBe true
    iter.close()
    tempFile.exists() shouldBe true // still alive: it2 is open
    it2.close()
    tempFile.exists() shouldBe false // now deleted
  }

  // --- AbstractRewindableEntityIterator (via InMemoryRewindableEntityIterator) ---

  behavior of "AbstractRewindableEntityIterator"

  it should "lazily call newIterator() only on first hasNext/next()" in {
    var callCount = 0
    val entities = Seq(entity(1))
    val iter = new AbstractRewindableEntityIterator {
      override def newIterator(): CloseableIterator[Entity] = {
        callCount += 1
        CloseableIterator(entities.iterator)
      }
    }
    callCount shouldBe 0
    iter.hasNext shouldBe true
    callCount shouldBe 1
    iter.hasNext shouldBe true
    callCount shouldBe 1 // no additional call
  }

  it should "allow re-use via newIterator() after close()" in {
    val entities = Seq(entity(1), entity(2))
    val iter = new InMemoryRewindableEntityIterator(entities)
    iter.toSeq shouldBe entities
    iter.close()
    // After close, the internal iterator is reset; a new iterator from newIterator() should work
    val fresh = iter.newIterator()
    fresh.toSeq shouldBe entities
  }

  // --- TempFileHolder reference counting ---

  behavior of "TempFileHolder"

  it should "keep the file alive while at least one instance is open" in {
    val entities = Seq(entity(1), entity(2))
    val iter = FileRewindableEntityIterator.load(iteratorOf(entities: _*), schema)
    val it2 = iter.newIterator().asInstanceOf[FileRewindableEntityIterator]

    val fileField = iter.getClass.getDeclaredField("file")
    fileField.setAccessible(true)
    val holder = fileField.get(iter).asInstanceOf[FileRewindableEntityIterator.TempFileHolder]
    val tempFile = holder()

    iter.close() // remove one instance
    tempFile.exists() shouldBe true // it2 still alive
    it2.close()
    tempFile.exists() shouldBe false
  }

  it should "delete the file after all instances are removed" in {
    val entities = Seq(entity(1))
    val iter = FileRewindableEntityIterator.load(iteratorOf(entities: _*), schema)

    val fileField = iter.getClass.getDeclaredField("file")
    fileField.setAccessible(true)
    val holder = fileField.get(iter).asInstanceOf[FileRewindableEntityIterator.TempFileHolder]
    val tempFile = holder()

    tempFile.exists() shouldBe true
    iter.close()
    tempFile.exists() shouldBe false
  }
}
