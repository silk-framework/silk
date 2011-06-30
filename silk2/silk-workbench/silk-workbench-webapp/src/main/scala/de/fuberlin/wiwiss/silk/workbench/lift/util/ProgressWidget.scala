package de.fuberlin.wiwiss.silk.workbench.lift.util

import net.liftweb.http.CometActor
import de.fuberlin.wiwiss.silk.util.Task
import net.liftweb.http.js.JE.JsRaw
import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.util.Task.Finished
import scala.xml.Text
import net.liftweb.http.js.JsCmds._

/**
 * A widget which displays a progressbar showing the current progress of a task.
 *
 * @param task The task for which the progress should be shown
 * @param hide Hide the widget if the task is not active.
 */
class ProgressWidget(task : Task[_], hide : Boolean = false) extends CometActor with Subscriber[Task.StatusMessage, Publisher[Task.StatusMessage]]
{
  task.subscribe(this)

  /** Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 1000L

  /** The time of the last update */
  private var lastUpdateTime = 0L

  override def notify(pub : Publisher[Task.StatusMessage], status : Task.StatusMessage)
  {
    if(status.isInstanceOf[Finished] || System.currentTimeMillis - lastUpdateTime > minUpdatePeriod )
    {
      val cmd = status match
      {
        case Task.Started()           => updateCmd
        case Task.StatusChanged(_, _) => updateCmd
        case Task.Finished(_, _)      => updateCmd
      }

      partialUpdate(cmd)

      lastUpdateTime = System.currentTimeMillis
    }
  }

  override def render =
  {
    <div id="progresswidget">
      <div id="progressbar"></div>
      <div class="progresstext" id="progresstext"></div>
      <div>{Script(OnLoad(updateCmd))}</div>
    </div>
  }

  private def updateCmd =
  {
    if(task.status.startsWith("Failed"))
    {
      JsShowId("progresswidget") &
      JsRaw("$('#progresswidget').attr('title', '" + task.status + "');") &
      JsRaw("$('#progressbar').progressbar({value: 0});").cmd &
      SetHtml("progresstext", Text("Failed to load cache"))
    }
    else
    {
      val showCmd =
        if(task.isRunning)
        {
          JsShowId("progresswidget")
        }
        else if(hide)
        {
          JsHideId("progresswidget")
        }
        else
        {
          JS.Empty
        }

      showCmd &
      JsRaw("$('#progresswidget').attr('title', '" + task.status + "');") &
      JsRaw("$('#progressbar').progressbar({value: " + (task.progress * 95 + 5) + "});").cmd &
      SetHtml("progresstext", Text(task.statusWithProgress))
    }
  }
}
