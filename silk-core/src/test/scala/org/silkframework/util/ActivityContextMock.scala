package org.silkframework.util

import org.silkframework.runtime.activity.{Activity, ActivityContext, ActivityControl, StatusHolder, UserContext, ValueHolder}

import java.util.logging.Logger

case class ActivityContextMock[T](initialValue: Option[T] = None) extends ActivityContext[T] {
  override def parent: Option[ActivityContext[_]] = None
  override def value: ValueHolder[T] = new ValueHolder(initialValue)
  override def status: StatusHolder = new StatusHolder()
  override def log: Logger = Logger.getAnonymousLogger
  override def child[R](activity: Activity[R], progressContribution: Double): ActivityControl[R] = ???
  override def blockUntil(condition: () => Boolean): Unit = ???
  override def helpQuiesce(): Unit = ???
  override def startedBy: UserContext = ???
}
