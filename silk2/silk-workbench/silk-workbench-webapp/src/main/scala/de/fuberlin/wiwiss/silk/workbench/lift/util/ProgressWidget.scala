package de.fuberlin.wiwiss.silk.workbench.lift.util

import net.liftweb.http.CometActor
import net.liftweb.http.js.JE.JsRaw
import collection.mutable.{Publisher, Subscriber}
import scala.xml.Text
import net.liftweb.http.js.JsCmds._
import de.fuberlin.wiwiss.silk.util.task._

/**
 * A widget which displays a progressbar showing the current progress of a task.
 *
 * @param task The task for which the progress should be shown
 * @param hide Hide the widget if the task is not active.
 */
class ProgressWidget(task: HasStatus, hide: Boolean = false) extends CometActor with Subscriber[Status, Publisher[Status]] {
  /**Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 1000L

  /**The time of the last update */
  private var lastUpdateTime = 0L

  task.subscribe(this)

  override def notify(pub: Publisher[Status], status: Status) {
    if (status.isInstanceOf[Finished] || System.currentTimeMillis - lastUpdateTime > minUpdatePeriod) {
      partialUpdate(updateCmd)
      lastUpdateTime = System.currentTimeMillis
    }
  }

  override def render = {
    <div id="progresswidget">
      <div id="progressbar"></div>
      <div class="progresstext" id="progresstext"></div>
      <div>
        {Script(OnLoad(updateCmd))}
      </div>
    </div>
  }

  private def updateCmd = task.status match {
    case Finished(_, false, _) => {
      JsShowId("progresswidget") &
        JsRaw("$('#progresswidget').attr('title', '" + task.status + "');") &
        JsRaw("$('#progressbar').progressbar({value: 0});").cmd &
        SetHtml("progresstext", Text("Failed to load cache"))
    }
    case _ => {
      val showCmd =
        if (task.isRunning) {
          JsShowId("progresswidget")
        }
        else if (hide) {
          JsHideId("progresswidget")
        }
        else {
          JS.Empty
        }

      showCmd &
        JsRaw("$('#progresswidget').attr('title', '" + task.status + "');") &
        JsRaw("$('#progressbar').progressbar({value: " + (task.status.progress * 95 + 5) + "});").cmd &
        SetHtml("progresstext", Text(task.toString))
    }
  }
}
