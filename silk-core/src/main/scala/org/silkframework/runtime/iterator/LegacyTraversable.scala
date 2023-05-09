package org.silkframework.runtime.iterator

import org.silkframework.runtime.execution.Execution
import org.silkframework.runtime.resource.DoSomethingOnGC

import java.util.concurrent.{ArrayBlockingQueue, ExecutionException, Future}

trait LegacyTraversable[T] extends CloseableIterator[T] with DoSomethingOnGC {

  private val queue = new ArrayBlockingQueue[T](100)

  // Load entities in the background
  private lazy val loadingFuture: Future[Unit] = LegacyTraversable.threadPool.submit[Unit](() => {
    foreach(queue.put)
  })

  def foreach[U](f: T => U): Unit

  override def hasNext: Boolean = {
    try {
      while (!loadingFuture.isDone) {
        checkForException()
        if (queue.peek() != null) {
          return true
        } else {
          Thread.sleep(100)
        }
      }
      checkForException()
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
      if(loadingFuture.isDone) {
        loadingFuture.get()
      }
    } catch {
      case ex: ExecutionException =>
        throw ex.getCause
    }
  }

  override def finalAction(): Unit = {
    close()
  }

  override def close(): Unit = {
    loadingFuture.cancel(true)
  }
}

object LegacyTraversable {

  private val threadPool = Execution.createThreadPool("LegacyTraversable")

}