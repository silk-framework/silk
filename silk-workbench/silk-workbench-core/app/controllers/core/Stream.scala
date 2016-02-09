package controllers.core

import models._
import org.silkframework.runtime.activity.{ActivityControl, Observable, Status}
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

  def status(statusObservable: Observable[Status]): Enumerator[Status] = {
    status(Traversable(statusObservable))
  }

  def status(statusObservables: Traversable[Observable[Status]]): Enumerator[Status] = {
    val (enumerator, channel) = Concurrent.broadcast[Status]
    val listener = new Listener[Status] {
      override def onUpdate(value: Status) {
        channel.push(value)
      }
    }
    for(status <- statusObservables) {
      // Push initial value
      channel.push(status())
      // Push updates
      status.onUpdate(listener)
    }
    listeners.put(enumerator, listener)
    enumerator
  }
}
