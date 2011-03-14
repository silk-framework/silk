package de.fuberlin.wiwiss.silk.util

import de.fuberlin.wiwiss.silk.util.Task._
import java.util.logging.{Level, Logger}
import collection.mutable.{Subscriber, Publisher}
import java.util.concurrent.{TimeUnit, ThreadPoolExecutor, Callable, Executors}

/**
 * A task which computes a result.
 * While executing the status of the execution can be queried.
 */
trait Task[+T] extends (() => T) with Publisher[StatusMessage]
{
  private val logger = Logger.getLogger(getClass.getName)

  private var currentStatus = "Idle"

  private var currentProgress = 0.0

  private var running = false

  var taskName = "Task"

  var logLevel = Level.INFO

  /**
   * Executes this task and returns the result.
   */
  override final def apply() : T = synchronized
  {
    running = true
    currentProgress = 0.0
    currentStatus = "Running"
    publish(Started())
    logger.log(logLevel, taskName + " started")

    try
    {
      val result = execute()
      running = false
      currentProgress = 1.0
      currentStatus = "Done"
      publish(Finished(true))
      logger.log(logLevel, taskName + " done")
      result
    }
    catch
    {
      case ex : Exception =>
      {
        running = false
        currentProgress = 1.0
        currentStatus = "Done"
        publish(Finished(false, Some(ex)))
        logger.log(logLevel, taskName + " failed", ex)
        throw ex
      }
    }
  }

  /**
   * Executes this task in a background thread
   */
  def runInBackground() : Future[T] =
  {
    running = true
    Task.backgroundExecutor.submit(toCallable(this))
  }

  /**
   * Tries to stop the execution of this task and blocks until the task has been stopped.
   * There is no guarantee that the task will stop immediately.
   * The default implementation blocks until the task has been finished.
   * Subclasses may override stopExecution() to allow cancellation of this task.
   */
  def cancel()
  {
    stopExecution()
  }

  /**
   * The current status of this task.
   */
  def status = currentStatus

  /**
   * The progress of the computation.
   * Will be 0.0 when the task has been started and 1.0 when it has finished execution.
   */
  def progress = currentProgress

  /**
   * The current status of this task including its progress.
   */
  def statusWithProgress = status + (if(isRunning) " (" + "%3.1f".format(progress * 100.0) + "%)" else "")

  /**
   *  True, if the task is running at the moment; False, otherwise.
   */
  def isRunning = running

  /**
   *  Must be overridden in subclasses to do the actual computation.
   */
  protected def execute() : T

  /**
   *  Can be overridden in subclasses to allow cancellation of the task.
   */
  protected def stopExecution() : Unit =
  {
    while(running) Thread.sleep(100)
  }

  protected def executeSubTask[R](subTask : Task[R], finalProgress : Double = 1.0) : R =
  {
    require(finalProgress >= progress, "finalProgress >= progress")

    //Disable logging of the subtask as this task will do the logging
    val subTaskLogLevel = subTask.logLevel
    subTask.logLevel = Level.FINEST

    //Subscribe to status changes of the sub task
    val subscriber = new Subscriber[StatusMessage, Publisher[StatusMessage]]
    {
      val initialProgress = progress

      override def notify(pub : Publisher[StatusMessage], event : StatusMessage)
      {
        event match
        {
          case StatusChanged(status, taskProgress) =>
          {
            updateStatus(status, initialProgress + taskProgress * (finalProgress - initialProgress))
          }
          case Finished(success, _) if success == true =>
          {
            updateStatus(finalProgress)
          }
          case _ =>
        }
      }
    }

    //Start sub task
    try
    {
      subTask.subscribe(subscriber)
      subTask()
    }
    finally
    {
      subTask.removeSubscription(subscriber)
      subTask.logLevel = subTaskLogLevel
    }
  }

  /**
   * Updates the status of this task.
   *
   * @param status The new status
   */
  protected def updateStatus(status : String)
  {
    currentStatus = status
    update()
  }

  /**
   * Updates the progress of this task.
   *
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  protected def updateStatus(progress : Double)
  {
    currentProgress = progress
    update()
  }

  /**
   * Updates the status of this task.
   *
   * @param status The new status
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  protected def updateStatus(status : String, progress : Double)
  {
    currentStatus = status
    currentProgress = progress
    update()
  }

  private def update()
  {
    if(logger.isLoggable(logLevel))
    {
      logger.log(logLevel, statusWithProgress)
    }
    publish(StatusChanged(currentStatus, currentProgress))
  }
}

object Task
{
  /**
   * Converts a task to a Java Runnable
   */
  implicit def toRunnable[T](task : Task[T]) = new Runnable { override def run() = task.apply() }

  /**
   * Converts a task to a Java Callable
   */
  implicit def toCallable[T](task : Task[T]) = new Callable[T] { override def call() = task.apply() }

  /**
   * The executor service used to execute link specs in the background.
   */
  private val backgroundExecutor =
  {
    val executor = Executors.newCachedThreadPool()

    //Reducing the keep-alive time of the executor, so it won't prevent the JVM from shutting down to long
    executor.asInstanceOf[ThreadPoolExecutor].setKeepAliveTime(2, TimeUnit.SECONDS)

    executor
  }

  /**
   * A status message
   */
  sealed trait StatusMessage

  /**
   * Message which indicates that the task has been started.
   */
  case class Started() extends StatusMessage

  /**
   * Message which indicates that the task has finished execution.
   *
   * @param success True, if the computation finished successfully. False, otherwise.
   * @param exception The exception, if the task failed.
   */
  case class Finished(success : Boolean, exception : Option[Exception] = None) extends StatusMessage

  /**
   * Message which notifies of a status change.
   *
   * @param status The new status
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  case class StatusChanged(status : String, progress : Double) extends StatusMessage
}
