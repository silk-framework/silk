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

  // Loads entities in the background
  private var loadingFuture: Option[Future[Unit]] = None

  def foreach[U](f: T => U): Unit

  override def retrieveNext(): Option[T] = {
    if(loadingFuture.isEmpty) {
      // Start retrieving entities from the traversable
      loadingFuture = Some(TraversableIterator.threadPool.submit[Unit](() => {
        foreach(queue.put)
      }))
    }
    // Retrieve next element from the queue
    try {
      while (!loadingFuture.get.isDone) {
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
        loadingFuture.get.cancel(true)
        throw ex
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

  override def close(): Unit = {
    for(future <- loadingFuture) {
      future.cancel(true)
    }
  }
}

object TraversableIterator {

  private val threadPool = Execution.createThreadPool("TraversableIterator")

}