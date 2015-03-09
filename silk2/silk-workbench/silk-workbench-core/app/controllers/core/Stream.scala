package controllers.core

import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityControl, Status}
import de.fuberlin.wiwiss.silk.runtime.oldtask.{HasStatus, TaskStatus}
import models._
import play.api.libs.iteratee.{Concurrent, Enumerator}
import scala.collection.mutable

object Stream {

  private val listeners = new mutable.WeakHashMap[Enumerator[_], Listener[_]]()

  def activityValue[T](activity: ActivityControl[T]): Enumerator[T] = {
    val (enumerator, channel) = Concurrent.broadcast[T]
    val listener = new Listener[T] {
      override def onUpdate(value: T) {
        channel.push(value)
      }
    }
    activity.value.onUpdate(listener)
    listeners.put(enumerator, listener)
    enumerator
  }

  def activityStatus(activity: ActivityControl[_]): Enumerator[Status] = {
    val (enumerator, channel) = Concurrent.broadcast[Status]
    val listener = new Listener[Status] {
      override def onUpdate(value: Status) {
        channel.push(value)
      }
    }
    activity.status.onUpdate(listener)
    listeners.put(enumerator, listener)
    enumerator
  }

  def currentTaskStatus[T <: HasStatus](taskHolder: TaskData[T]): Enumerator[TaskStatus] = {
    val (enumerator, channel) = Concurrent.broadcast[TaskStatus]

    lazy val listener = new CurrentTaskStatusListener(taskHolder) {
      def onUpdate(status: TaskStatus) {
        channel.push(status)
      }
    }

    listeners.put(enumerator, listener)
    enumerator
  }

  def currentStatus(taskControl: TaskData[ActivityControl[_]]): Enumerator[Status] = {
    val (enumerator, channel) = Concurrent.broadcast[Status]

    lazy val listener = new CurrentStatusListener(taskControl) {
      def onUpdate(status: Status) {
        channel.push(status)
      }
    }

    listeners.put(enumerator, listener)
    enumerator
  }

  def taskStatus(task: HasStatus) = {
    val (enumerator, channel) = Concurrent.broadcast[TaskStatus]

    lazy val listener = new TaskStatusListener(task) {
      def onUpdate(status: TaskStatus) {
        channel.push(status)
      }
    }

    listeners.put(enumerator, listener)
    Enumerator(task.status) andThen enumerator
  }

  def taskData[T](userData: TaskData[T]): Enumerator[T] = {
    val (enumerator, channel) = Concurrent.broadcast[T]

    lazy val listener = new TaskDataListener(userData) {
      def onUpdate(value: T) {
        channel.push(value)
      }
    }

    listeners.put(enumerator, listener)
    enumerator
  }
}
