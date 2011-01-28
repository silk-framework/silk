package de.fuberlin.wiwiss.silk.workbench.lift.util

import net.liftweb.http.CometActor
import de.fuberlin.wiwiss.silk.util.Task
import net.liftweb.http.js.JsCmds.{Script, OnLoad, SetHtml}
import net.liftweb.http.js.JE.JsRaw
import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.util.Task.Finished
import scala.xml.Text

/**
 * A widget which displays a progressbar showing the current progress of a task.
 */
class ProgressWidget(task : Task[_]) extends CometActor with Subscriber[Task.StatusMessage, Publisher[Task.StatusMessage]]
{
  task.subscribe(this)

  /** Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 1000L

  /** The time of the last update */
  private var lastUpdateTime = 0L

  override def notify(pub : Publisher[Task.StatusMessage], status : Task.StatusMessage)
  {
    def update(status : String) = partialUpdate(progressbarJS & SetHtml("progresstext", Text(status)))

    if(status.isInstanceOf[Finished] || System.currentTimeMillis - lastUpdateTime > minUpdatePeriod )
    {
      status match
      {
        case Task.Started() => update("Started")
        case Task.StatusChanged(_, _) => update(task.statusWithProgress)
        case Task.Finished(_, _) => update("Done")
      }

      lastUpdateTime = System.currentTimeMillis
    }
  }

  override def render =
  {
    <div id="progressbar"></div>
    <div class="progresstext" id="progresstext">{task.statusWithProgress}</div>
    <div>{Script(OnLoad(progressbarJS))}</div>
  }

  private def progressbarJS = JsRaw("$('#progressbar').progressbar({value: " + (task.progress * 95 + 5) + "});").cmd
}
