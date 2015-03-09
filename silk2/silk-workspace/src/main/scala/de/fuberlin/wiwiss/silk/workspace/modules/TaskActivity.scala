package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, Activity}

import scala.reflect.ClassTag


case class TaskActivity[T](create: () => Activity[T]) extends Activity[T] {

  val activityType = create().getClass

  override def initialValue = create().initialValue

  /**
   * Executes this activity.
   *
   * @param context Holds the context in which the activity is executed.
   */
  override def run(context: ActivityContext[T]): Unit = {
    val activity = create()
    context.executeBlocking(activity, 1.0, context.value.update)
  }
}