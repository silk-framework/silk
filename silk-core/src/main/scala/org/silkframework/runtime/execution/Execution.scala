package org.silkframework.runtime.execution

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.{Level, Logger}
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor

import scala.math.max

/**
  * Creates new thread pools.
  * Use the factory methods in this object instead of creating thread pools directly.
  * Takes care of naming threads and logging uncaught exceptions.
  */
object Execution {

  /** Logger for uncaught exceptions */
  private val log = Logger.getLogger(getClass.getName)

  /** Prefix to be prepended to thread names */
  private val threadPrefix = "Silk-"

  /**
    * Creates a new fork/join pool.
    * The size of the thread pool is determined by the number of available processors.
    *
    * @param name A label to be used for naming threads. Helps debugging and finding threads that belong to this pool.
    */
  def createForkJoinPool(name: String): ForkJoinPool = {
    val minimumNumberOfThreads = 4
    val threadCount = max(minimumNumberOfThreads, Runtime.getRuntime.availableProcessors())
    new ForkJoinPool(threadCount, new PrefixedForkJoinWorkerThreadFactory(name), null, true)
  }

  /**
    * Creates a new unlimited cached thread pool.
    *
    * @param name A label to be used for naming threads. Helps debugging and finding threads that belong to this pool.
    */
  def createThreadPool(name: String): ExecutorService = {
    Executors.newCachedThreadPool( new PrefixedThreadFactory(name))
  }

  /**
    * Creates a new fixed thread pool.
    *
    * @param name A label to be used for naming threads. Helps debugging and finding threads that belong to this pool.
    * @param numberOfThreads The number of threads in the pool
    */
  def createFixedThreadPool(name: String,
                            numberOfThreads: Int,
                            workQueue: BlockingQueue[Runnable] = new LinkedBlockingQueue[Runnable](),
                            rejectedExecutionHandler: Option[RejectedExecutionHandler] = None): ExecutorService = {
    val tpe = new ThreadPoolExecutor(numberOfThreads,
      numberOfThreads,
      0L,
      TimeUnit.MILLISECONDS,
      workQueue,
      new PrefixedThreadFactory(name))
    rejectedExecutionHandler.foreach(tpe.setRejectedExecutionHandler)
    tpe
  }

  /**
    * Creates a new scheduled thread pool.
    *
    * @param name A label to be used for naming threads. Helps debugging and finding threads that belong to this pool.
    * @param corePoolSize The number of threads in the pool
    */
  def createScheduledThreadPool(name: String, corePoolSize: Int): ScheduledExecutorService = {
    Executors.newScheduledThreadPool(corePoolSize, new PrefixedThreadFactory(name))
  }

  /**
    * Thread factory that names threads using a prefix and a count.
    * Also makes sure that uncaught exceptions are logged.
    */
  private class PrefixedThreadFactory(prefix: String) extends ThreadFactory {

    private val threadNumber = new AtomicInteger(1)

    override def newThread(r: Runnable): Thread = {
      val t = new Thread(null, r, threadPrefix + prefix + threadNumber.getAndIncrement, 0)
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
      t.setName(threadPrefix + prefix + threadNumber.getAndIncrement)
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
