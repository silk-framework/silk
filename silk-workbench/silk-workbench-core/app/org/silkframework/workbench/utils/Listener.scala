package org.silkframework.workbench.utils

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import org.silkframework.runtime.execution.Execution

/**
  * A Listener that limits that rate of updates.
  */
trait Listener[T] extends (T => Unit) {

  /** The minimum number of milliseconds between two successive calls to onUpdate. */
  var maxFrequency = 500

  /** The time of the last call to onUpdate */
  @volatile private var lastUpdateTime = 0L

  /** Indicates if a call to onUpdate is scheduled */
  @volatile private var scheduled = false

  /** The last message */
  @volatile private var lastMessage: Option[T] = None

  private val logger = Logger.getLogger(getClass.getName)

  def apply(value: T): Unit = {
    if(scheduled) {
      lastMessage = Some(value)
    } else {
      val time = System.currentTimeMillis() - lastUpdateTime
      if (time > maxFrequency) {
        onUpdate(value)
        lastUpdateTime = System.currentTimeMillis()
      } else {
        scheduled = true
        lastMessage = Some(value)
        delayedUpdate(maxFrequency)
      }
    }
  }

  protected def onUpdate(value: T)

  private def delayedUpdate(delay: Long) {
    Listener.executor.schedule(new Runnable {
      def run() {
        try {
          scheduled = false
          lastUpdateTime = System.currentTimeMillis()
          onUpdate(lastMessage.get)
        } catch {
          case ex: Exception => logger.log(Level.WARNING, "Error on update", ex)
        }
      }
    }, delay, TimeUnit.MILLISECONDS)
  }
}

object Listener {
  private val executor = Execution.createScheduledThreadPool(getClass.getSimpleName, 1)
}