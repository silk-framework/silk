package org.silkframework.execution

import java.util.concurrent.atomic.AtomicBoolean
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.runtime.iterator.{CloseableIterator, LegacyTraversable}

class InterruptibleTraversableTest extends AnyFlatSpec with Matchers {
  behavior of "Interruptible Traversable"

  private val SHORT_TIME = 50

  it should "stop traversing when Thread is interrupted" in {
    // Set when the first item is emitted by the traversable
    val startedTraversing = new AtomicBoolean(false)
    val unlimitedTraversable = traverseEndlessly(startedTraversing)
    // Set when the thread finished consuming the traversable
    val ended = new AtomicBoolean(false)
    val t = traversableConsumingThread(unlimitedTraversable, ended)
    t.start()
    while(!startedTraversing.get()) {
      waitShortly()
    }
    t.interrupt()
    while(!ended.get()) {
      waitShortly()
    }
    ended.get() mustBe true
  }

  private def traversableConsumingThread(unlimitedTraversable: CloseableIterator[Int],
                                         ended: AtomicBoolean): Thread = {
    new Thread {
      override def run(): Unit = {
        var sum = 0L
        try {
          for (i <- unlimitedTraversable) {
            sum += i
          }
        } catch {
          case _: InterruptedException => // Ignore
        }
        ended.set(true)
      }
    }
  }

  private def traverseEndlessly(startedIterating: AtomicBoolean): CloseableIterator[Int] = {
    new InterruptibleIterator[Int](
      new LegacyTraversable[Int] {
        override def foreach[U](f: Int => U): Unit = {
          var i = 0
          while (true) {
            i += 1
            f(i)
            startedIterating.set(true)
          }
        }
      }
    )
  }

  private def waitShortly(): Unit = {
    Thread.sleep(SHORT_TIME)
  }
}
