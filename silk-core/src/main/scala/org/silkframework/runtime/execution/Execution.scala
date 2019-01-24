package org.silkframework.runtime.execution

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.{Level, Logger}

import scala.math.max

object Execution {

  private val log = Logger.getLogger(getClass.getName)

  /**
    * Creates a new fork/join pool.
    * The size of the thread pool is determined by the number of available processors.
    *
    * @param name A label to be used for naming threads. Helps debugging and finding threads that belong to this pool.
    */
  def createForkJoinPool(name: String): ForkJoinPool = {
    val minimumNumberOfThreads = 4
    val threadCount = max(minimumNumberOfThreads, Runtime.getRuntime.availableProcessors())
    new ForkJoinPool(threadCount, new PrefixedForkJoinWorkerThreadFactory(name + "-thread-"), null, true)
  }

  /**
    * Creates a new unlimited cached thread pool.
    *
    * @param name A label to be used for naming threads. Helps debugging and finding threads that belong to this pool.
    */
  def createThreadPool(name: String): ExecutorService = {
    Executors.newCachedThreadPool( new PrefixedThreadFactory(name + "-thread-"))
  }

  /**
    * Thread factory that names threads using a prefix and a count.
    * Also makes sure that uncaught exceptions are logged.
    */
  private class PrefixedThreadFactory(prefix: String) extends ThreadFactory {

    private val threadNumber = new AtomicInteger(1)

    override def newThread(r: Runnable): Thread = {
      val t = new Thread(null, r, prefix + threadNumber.getAndIncrement, 0)
      if (t.isDaemon) t.setDaemon(false)
      if (t.getPriority != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY)
      t.setUncaughtExceptionHandler(LoggingExceptionHandler)
      t
    }
  }

  /**
    * ForkJoinWorkerThreadFactory that names threads using a prefix and a count.
    * Also makes sure that uncaught exceptions are logged.
    */
  private class PrefixedForkJoinWorkerThreadFactory(prefix: String) extends ForkJoinWorkerThreadFactory {

    private val factory = ForkJoinPool.defaultForkJoinWorkerThreadFactory

    private val threadNumber = new AtomicInteger(1)

    override def newThread(pool: ForkJoinPool): ForkJoinWorkerThread = {
      val t = factory.newThread(pool)
      t.setName(prefix + threadNumber.getAndIncrement)
      t.setUncaughtExceptionHandler(LoggingExceptionHandler)
      t
    }
  }

  /**
    * Exception handler that logs uncaught exceptions.
    */
  private object LoggingExceptionHandler extends UncaughtExceptionHandler {
    override def uncaughtException(t: Thread, e: Throwable): Unit = {
      log.log(Level.WARNING, s"Thread ${t.getName} failed with an exception", e)
    }
  }

}
