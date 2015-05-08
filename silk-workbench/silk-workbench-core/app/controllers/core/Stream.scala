package controllers.core

import de.fuberlin.wiwiss.silk.runtime.activity.{Observable, ActivityControl, Status}
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

  def status(status: Observable[Status]): Enumerator[Status] = {
    val (enumerator, channel) = Concurrent.broadcast[Status]
    val listener = new Listener[Status] {
      override def onUpdate(value: Status) {
        channel.push(value)
      }
    }
    status.onUpdate(listener)
    listeners.put(enumerator, listener)
    enumerator
  }
}
