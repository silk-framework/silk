package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, Activity}

class TaskActivity[T](override val initialValue: T, create: T => Activity[T]) extends Activity[T] {

  val activityType = create(initialValue).getClass

  override def name = create(initialValue).name

  /**
   * Executes this activity.
   *
   * @param context Holds the context in which the activity is executed.
   */
  override def run(context: ActivityContext[T]): Unit = {
    val activity = create(context.value())
    context.executeBlocking(activity, 1.0, context.value.update)
  }
}

object TaskActivity {

  def apply(create: => Activity[Unit]) = new TaskActivity[Unit](Unit, (Unit) => create)

  def apply[T](initialValue: T, create: T => Activity[T]) = new TaskActivity[T](initialValue, create)

}