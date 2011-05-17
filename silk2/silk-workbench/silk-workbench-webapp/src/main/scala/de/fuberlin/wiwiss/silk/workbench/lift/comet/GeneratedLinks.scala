package de.fuberlin.wiwiss.silk.workbench.lift.comet

import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.util.Task
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.output.Link
class GeneratedLinks extends LinkList
{
  /** Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 3000L

  /** The time of the last update */
  private var lastUpdateTime = 0L

  /** Register to status messages of the evaluation task in order to be notified when new links are available */
  User().evaluationTask.subscribe(new Subscriber[Task.StatusMessage, Publisher[Task.StatusMessage]]
  {
    def notify(pub : Publisher[Task.StatusMessage], status : Task.StatusMessage)
    {
      status match
      {
        case Task.Started() =>
        {
        }
        case Task.StatusChanged(_, _) if System.currentTimeMillis - lastUpdateTime > minUpdatePeriod =>
        {
          partialUpdate(updateLinks)
          lastUpdateTime = System.currentTimeMillis
        }
        case Task.Finished(_, _) =>
        {
          partialUpdate(updateLinks)
        }
        case _ =>
      }
    }
  })

  override protected def linkCount : Int =
  {
    User().evaluationTask.links.size
  }

  override protected def links(from : Int, until : Int) : Traversable[(Link, Int)] =
  {
    val linkingTask = User().linkingTask
    def alignment = linkingTask.alignment

    for(link <- User().evaluationTask.links.view(from, until)) yield
    {
      if(alignment.positive.contains(link))
      {
        (link, 1)
      }
      else if(alignment.negative.contains(link))
      {
        (link, -1)
      }
      else
      {
        (link, 0)
      }
    }
  }
}
