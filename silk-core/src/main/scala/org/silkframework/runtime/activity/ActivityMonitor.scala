package org.silkframework.runtime.activity
import java.util.logging.Logger

/**
  * Holds the current status and value of an activity, but does not control its execution.
  *
  * @param name The name of this activity.
  * @param parent The parent activity.
  * @param progressContribution The factor by which the progress of this activity contributes to the progress of the provided parent activity.
  * @param initialValue The initial value of this activity.
  * @tparam T The value type. Set to [[Unit]] if no values are generated.
  */
class ActivityMonitor[T](name: String, parent: Option[ActivityContext[_]] = None, progressContribution: Double = 0.0, initialValue: => Option[T] = None) extends ActivityContext[T] {

  /**
    * Holds all current child activities.
    */
  @volatile
  private var childControls: Seq[ActivityControl[_]] = Seq.empty

  /**
    * Retrieves the logger to be used by the activity.
    */
  override val log: Logger = parent match {
    case None => Logger.getLogger(Activity.loggingPath + "." + name)
    case Some(p) => Logger.getLogger(p.log.getName + "." + name)
  }

  /**
    * Holds the current value.
    */
  override val value: ValueHolder[T] = new ValueHolder[T](initialValue)

  /**
    * Retrieves current status of the activity.
    */
  override val status: StatusHolder = new StatusHolder(log, parent.map(_.status), progressContribution)

  /**
    * Adds a child activity.
    *
    * @param activity             The child activity to be added.
    * @param progressContribution The factor by which the progress of the child activity contributes to the progress of this
    *                             activity. A factor of 0.1 means the when the child activity is finished,the progress of the
    *                             parent activity is advanced by 0.1.
    * @return The activity control for the child activity.
    */
  override def child[R](activity: Activity[R], progressContribution: Double): ActivityControl[R] = {
    val execution = new ActivityExecution(activity, Some(this), progressContribution)
    addChild(execution)
    execution
  }

  /**
    * The current children of this activity.
    */
  def children(): Seq[ActivityControl[_]] = {
    removeDoneChildren()
    childControls
  }

  /**
    * Adds a new child activity.
    */
  private def addChild(control: ActivityControl[_]): Unit = {
    childControls = childControls :+ control
  }

  /**
    * Removes all finished child activities.
    */
  private def removeDoneChildren(): Unit = {
    childControls = childControls.filter(_.status().isRunning)
  }
}
