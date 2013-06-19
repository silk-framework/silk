package controllers.util

import java.util.IdentityHashMap
import play.api.libs.iteratee.{Concurrent, Enumerator}
import de.fuberlin.wiwiss.silk.util.task.{ValueTask, HasStatus, TaskStatus}
import models._

object Stream {
  private val listeners = new IdentityHashMap[Listener[_], Unit]()

  def currentTaskStatus[T <: HasStatus](taskHolder: TaskData[T]) = {
    lazy val events = Enumerator.imperative[TaskStatus](onStart, onComplete)

    lazy val listener = new CurrentTaskStatusListener(taskHolder) {
      def onUpdate(status: TaskStatus) {
        events.push(status)
      }
    }

    def onStart() {
      listeners.put(listener, Unit)
    }

    def onComplete() {
      listeners.remove(listener)
    }

    events
  }

  def currentTaskValue[T](taskHolder: TaskData[_ <: ValueTask[T]]) = {
    lazy val events = Enumerator.imperative[T](onStart, onComplete)

    lazy val listener = new CurrentTaskValueListener(taskHolder) {
      def onUpdate(value: T) {
        events.push(value)
      }
    }

    def onStart() {
      listeners.put(listener, Unit)
    }

    def onComplete() {
      listeners.remove(listener)
    }

    events
  }

  def taskStatus(task: HasStatus) = {
    lazy val events = Enumerator.imperative[TaskStatus](onStart, onComplete)

    lazy val listener = new TaskStatusListener(task) {
      def onUpdate(status: TaskStatus) {
        events.push(status)
      }
    }

    def onStart() {
      listeners.put(listener, Unit)
    }

    def onComplete() {
      listeners.remove(listener)
    }

    Enumerator(task.status) andThen events
  }

  def taskData[T](userData: TaskData[T]) = {
    lazy val events = Enumerator.imperative[T](onStart, onComplete)

    lazy val listener = new TaskDataListener(userData) {
      def onUpdate(value: T) {
        events.push(value)
      }
    }

    def onStart() {
      listeners.put(listener, Unit)
    }

    def onComplete() {
      listeners.remove(listener)
    }

    events
  }
}
