package de.fuberlin.wiwiss.silk.workbench.lift.util

import net.liftweb.http.CometActor
import net.liftweb.http.js.JE.JsRaw
import scala.xml.Text
import net.liftweb.http.js.JsCmds._
import de.fuberlin.wiwiss.silk.util.task._
import de.fuberlin.wiwiss.silk.util.Observable

/**
 * A widget which displays a progressbar showing the current progress of a task.
 *
 * @param task The task for which the progress should be shown
 * @param hide Hide the widget if the task is not active.
 */
class ProgressWidget(val task: Observable[Status], hide: Boolean = false) extends CometActor {
  /**Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 1000L

  /**The time of the last update */
  private var lastUpdateTime = 0L

  private var currentStatus: Status = Idle()

  override protected val dontCacheRendering = true

  task.onUpdate(TaskListener)

  override def render = {
    <div id="progresswidget">
      <div id="progressbar"></div>
      <div class="progresstext" id="progresstext"></div>
      <div>
        {Script(OnLoad(updateCmd(currentStatus)))}
      </div>
    </div>
  }

  private def updateCmd(status: Status) = status match {
    case Finished(_, false, _) => {
      JsShowId("progresswidget") &
      JsRaw("$('#progresswidget').attr('title', '" + status + "');") &
      JsRaw("$('#progressbar').progressbar({value: 0});").cmd &
      SetHtml("progresstext", Text("Failed to load cache"))
    }
    case _ => {
      val showCmd = status match {
        case _: Idle | _: Finished if hide => JsHideId("progresswidget")
        case _ => JsShowId("progresswidget")
      }

      showCmd &
      JsRaw("$('#progresswidget').attr('title', '" + status + "');") &
      JsRaw("$('#progressbar').progressbar({value: " + (status.progress * 95 + 5) + "});").cmd &
      SetHtml("progresstext", Text(status.toString))
    }
  }

  private object TaskListener extends (Status => Unit) {
    def apply(status: Status) {
      if (status.isInstanceOf[Finished] || status.isInstanceOf[Canceled] || System.currentTimeMillis - lastUpdateTime > minUpdatePeriod) {
        currentStatus = status
        partialUpdate(updateCmd(status))
        lastUpdateTime = System.currentTimeMillis
      }
    }
  }
}
