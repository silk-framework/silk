package de.fuberlin.wiwiss.silk.util.task

import de.fuberlin.wiwiss.silk.util.task.Task._
import java.util.logging.Level
import collection.mutable.{Subscriber, Publisher}
import java.util.concurrent.{TimeUnit, ThreadPoolExecutor, Callable, Executors}

/**
 * A task which computes a result.
 * While executing the status of the execution can be queried.
 */
trait Task[+T] extends HasStatus with (() => T) {

  var taskName = "Task"

  /**
   * Executes this task and returns the result.
   */
  override final def apply(): T = synchronized {
    updateStatus(Started(taskName))

    try {
      val result = execute()
      updateStatus(Finished(taskName, true))
      result
    } catch {
      case ex: Exception => {
        updateStatus(Finished(taskName, false, Some(ex)))
        throw ex
      }
    }
  }

  /**
   * Executes this task in a background thread
   */
  def runInBackground(): Future[T] = {
    Task.backgroundExecutor.submit(toCallable(this))
  }

  /**
   * Tries to stop the execution of this task and blocks until the task has been stopped.
   * There is no guarantee that the task will stop immediately.
   * The default implementation blocks until the task has been finished.
   * Subclasses may override stopExecution() to allow cancellation of this task.
   */
  def cancel() {
    stopExecution()
    updateStatus(Idle())
  }

  /**
   *  Must be overridden in subclasses to do the actual computation.
   */
  protected def execute(): T

  /**
   *  Can be overridden in subclasses to allow cancellation of the task.
   */
  protected def stopExecution() {
    while (isRunning) Thread.sleep(100)
  }

  protected def executeSubTask[R](subTask: Task[R], finalProgress: Double = 1.0): R = {
    require(finalProgress >= status.progress, "finalProgress >= progress")

    //Disable logging of the subtask as this task will do the logging
    val subTaskLogLevel = subTask.logLevel
    subTask.logLevel = Level.FINEST

    //Subscribe to status changes of the sub task
    val subscriber = new Subscriber[Status, Publisher[Status]] {
      val initialProgress = status.progress

      override def notify(pub: Publisher[Status], event: Status) {
        event match {
          case Running(status, taskProgress) => {
            updateStatus(status, initialProgress + taskProgress * (finalProgress - initialProgress))
          }
          case Finished(_, success, _) if success == true => {
            updateStatus(finalProgress)
          }
          case _ =>
        }
      }
    }

    //Start sub task
    try {
      subTask.subscribe(subscriber)
      subTask()
    } finally {
      subTask.removeSubscription(subscriber)
      subTask.logLevel = subTaskLogLevel
    }
  }
}

object Task {
  /**
   * Converts a task to a Java Runnable
   */
  implicit def toRunnable[T](task: Task[T]) = new Runnable {
    override def run() = task.apply()
  }

  /**
   * Converts a task to a Java Callable
   */
  implicit def toCallable[T](task: Task[T]) = new Callable[T] {
    override def call() = task.apply()
  }

  /**
   * The executor service used to execute link specs in the background.
   */
  private val backgroundExecutor = {
    val executor = Executors.newCachedThreadPool()

    //Reducing the keep-alive time of the executor, so it won't prevent the JVM from shutting down to long
    executor.asInstanceOf[ThreadPoolExecutor].setKeepAliveTime(2, TimeUnit.SECONDS)

    executor
  }
}
