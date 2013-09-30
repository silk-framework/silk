package controllers.util

import java.util.IdentityHashMap
import play.api.libs.iteratee.{Concurrent, Enumerator}
import de.fuberlin.wiwiss.silk.util.task.{ValueTask, HasStatus, TaskStatus}
import models._
import scala.concurrent.ExecutionContext.Implicits.global

object Stream {
  private val listeners = new IdentityHashMap[Listener[_], Unit]()

  def currentTaskStatus[T <: HasStatus](taskHolder: TaskData[T]): Enumerator[TaskStatus] = {
    val (enumerator, channel) = Concurrent.broadcast[TaskStatus]

    lazy val listener = new CurrentTaskStatusListener(taskHolder) {
      def onUpdate(status: TaskStatus) {
        channel.push(status)
      }
    }

    listeners.put(listener, Unit)
    enumerator.onDoneEnumerating(() => listeners.remove(listener))
  }

  def currentTaskValue[T](taskHolder: TaskData[_ <: ValueTask[T]]): Enumerator[T] = {
    val (enumerator, channel) = Concurrent.broadcast[T]

    lazy val listener = new CurrentTaskValueListener(taskHolder) {
      def onUpdate(value: T) {
        channel.push(value)
      }
    }

    listeners.put(listener, Unit)
    enumerator.onDoneEnumerating(() => listeners.remove(listener))
  }

  def taskStatus(task: HasStatus) = {
    val (enumerator, channel) = Concurrent.broadcast[TaskStatus]

    lazy val listener = new TaskStatusListener(task) {
      def onUpdate(status: TaskStatus) {
        channel.push(status)
      }
    }

    listeners.put(listener, Unit)
    val closingEnumerator = enumerator.onDoneEnumerating(() => listeners.remove(listener))

    Enumerator(task.status) andThen closingEnumerator
  }

  def taskData[T](userData: TaskData[T]): Enumerator[T] = {
    val (enumerator, channel) = Concurrent.broadcast[T]

    lazy val listener = new TaskDataListener(userData) {
      def onUpdate(value: T) {
        channel.push(value)
      }
    }

    listeners.put(listener, Unit)
    enumerator.onDoneEnumerating(() => listeners.remove(listener))
  }
}
