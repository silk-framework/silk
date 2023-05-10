package org.silkframework.runtime.iterator

import org.silkframework.runtime.execution.Execution
import org.silkframework.runtime.resource.DoSomethingOnGC

import java.util.concurrent.{ArrayBlockingQueue, ExecutionException, Future}

/**
  * A closable iterator that is implemented by a single foreach function.
  * This class is to support legacy code that was based on the obsolete Scala Traversable classes.
  * New code should preferably implement CloseableIterator directly.
  */
trait TraversableIterator[T] extends CloseableIterator[T] with DoSomethingOnGC {

  protected val bufferSize = 100

  private val queue = new ArrayBlockingQueue[T](bufferSize)

  // Load entities in the background
  private lazy val loadingFuture: Future[Unit] = TraversableIterator.threadPool.submit[Unit](() => {
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

object TraversableIterator {

  private val threadPool = Execution.createThreadPool("TraversableIterator")

}