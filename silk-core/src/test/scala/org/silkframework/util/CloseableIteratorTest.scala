package org.silkframework.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CloseableIteratorTest extends AnyFlatSpec with Matchers {

  behavior of "CloseableIterator"

  it should "auto close after full iteration" in {
    val iterator = new TestIterator with AutoClose[Int]
    iterator.size shouldBe 10
    iterator.hasBeenClosed shouldBe true
  }

  private class TestIterator extends CloseableIterator[Int] {

    private val numbers = Iterator.iterate(0)(_ + 1).take(10)

    var hasBeenClosed: Boolean = false

    override def hasNext: Boolean = numbers.hasNext

    override def next(): Int = numbers.next()

    override def close(): Unit = {
      hasBeenClosed = true
    }

  }

}
