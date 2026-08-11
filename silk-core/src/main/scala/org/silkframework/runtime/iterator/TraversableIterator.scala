package org.silkframework.runtime.iterator

import org.silkframework.runtime.execution.Execution
import org.silkframework.runtime.resource.DoSomethingOnGC

import java.util.concurrent.{ArrayBlockingQueue, ExecutionException, Future, TimeUnit}

/**
  * A closable iterator that is implemented by a single foreach function.
  * This class is to support legacy code that was based on the obsolete Scala Traversable classes.
  * New code should preferably implement CloseableIterator directly.
  */
trait TraversableIterator[T] extends BufferingIterator[T] with DoSomethingOnGC {

  protected val bufferSize = 100

  private val queue = new ArrayBlockingQueue[T](bufferSize)

  // Guards the loadingFuture lifecycle
  private val loadingLock = new Object

  // Loads entities in the background. Mutated only under loadingLock
  @volatile
  private var loadingFuture: Option[Future[Unit]] = None

  @volatile
  private var closed = false

  def foreach[U](f: T => U): Unit

  override def retrieveNext(): Option[T] = {
    startLoading() match {
      case None =>
        None // Closed before loading started, so nothing will be produced.
      case Some(future) =>
        try {
          while (!future.isDone) {
            val nextElement = queue.poll(100, TimeUnit.MILLISECONDS)
            if (nextElement != null) {
              checkForException()
              return Some(nextElement)
            }
          }
          checkForException()
          Option(queue.poll())
        } catch {
          case ex: InterruptedException =>
            future.cancel(true)
            throw ex
        }
    }
  }

  // Returns the background loading future, starting it on the first call. Returns None if already closed.
  private def startLoading(): Option[Future[Unit]] = {
    loadingFuture match {
      case started @ Some(_) => started // Fast path: already loading, no need to lock.
      case None =>
        loadingLock.synchronized {
          if (loadingFuture.isEmpty && !closed) {
            loadingFuture = Some(TraversableIterator.threadPool.submit[Unit](() => foreach(queue.put)))
          }
          loadingFuture
        }
    }
  }

  private def checkForException(): Unit = {
    try {
      for(future <- loadingFuture if future.isDone) {
        future.get()
      }
    } catch {
      case ex: ExecutionException =>
        throw ex.getCause
    }
  }

  override def finalAction(): Unit = {
    close()
  }

  override def close(): Unit = loadingLock.synchronized {
    closed = true
    for(future <- loadingFuture) {
      future.cancel(true)
    }
  }
}

object TraversableIterator {

  private val threadPool = Execution.createThreadPool("TraversableIterator")

}