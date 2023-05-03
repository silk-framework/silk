package org.silkframework.util

import org.silkframework.runtime.execution.Execution
import org.silkframework.runtime.resource.DoSomethingOnGC

import java.util.concurrent.{ArrayBlockingQueue, ExecutionException, Future}

trait LegacyTraversable[T] extends Iterable[T] {

  def foreach[U](f: T => U): Unit

  override def iterator: Iterator[T] = {
    new LegacyTraversableIterator[T](this)
  }
}


private class LegacyTraversableIterator[T](traversable: LegacyTraversable[T]) extends Iterator[T] with DoSomethingOnGC {

  private val queue = new ArrayBlockingQueue[T](100)

  // Load entities in the background
  private val loadingFuture: Future[Unit] = LegacyTraversableIterator.threadPool.submit[Unit](() => {
    //TODO check for interruption
    traversable.foreach(queue.put)
  })

  override def hasNext: Boolean = {
    try {
      while (!loadingFuture.isDone) {
        if (queue.peek() != null) {
          return true
        } else {
          Thread.sleep(100)
        }
      }
      queue.peek() != null
    } catch {
      case ex: InterruptedException =>
        loadingFuture.cancel(true)
        throw ex
    }
  }

  override def next(): T = {
    try {
      while (!loadingFuture.isDone) {
        val nextElement = queue.poll()
        if (nextElement != null) {
          checkForException()
          return nextElement
        } else {
          Thread.sleep(100)
        }
      }
      checkForException()
      val nextElement = queue.poll()
      if(nextElement == null) {
        throw new NoSuchElementException
      }
      nextElement
    } catch {
      case ex: InterruptedException =>
        loadingFuture.cancel(true)
        throw ex
    }
  }

  private def checkForException(): Unit = {
    try {
      loadingFuture.get()
    } catch {
      case ex: ExecutionException =>
        throw ex.getCause
    }

  }

  override def finalAction(): Unit = {
    loadingFuture.cancel(true)
  }
}

object LegacyTraversableIterator {

  private val threadPool = Execution.createThreadPool("LegacyTraversableIterator")

}