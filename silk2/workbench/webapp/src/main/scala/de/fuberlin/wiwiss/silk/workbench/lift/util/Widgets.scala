package de.fuberlin.wiwiss.silk.workbench.lift.util

import de.fuberlin.wiwiss.silk.util.Task
import net.liftweb.http.SHtml

import net.liftweb.http.js.JsCmds.{SetHtml, Script, OnLoad}
import JavaScriptUtils.PeriodicUpdate
import net.liftweb.http.js.JE.JsRaw
import scala.xml.Text

object Widgets
{
  def taskControl[T](task : Task[T]) =
  {
    def startTask()
    {
      if(!task.isRunning)
      {
        task.runInBackground()
      }
    }

    <form method="POST">
      {SHtml.submit("Start", startTask)}
    </form>
  }

  def taskProgress[T](task : Task[T]) =
  {
    currentTaskProgress(() => Some(task))
  }

  def currentTaskProgress[T](task : () => Option[Task[T]]) =
  {
    //Updates the status message
    def update() = task() match
    {
      case Some(currentTask) =>
      {
        val html =
        {
          <div id="progressbar"></div>
          <div class="progresstext">{currentTask.statusWithProgress}</div>
        }

        val javascript = "$('#progressbar').progressbar({value: " + (currentTask.progress * 95 + 5) + "});"

        SetHtml("status", html) & JsRaw(javascript).cmd
      }
      case None =>
      {
        SetHtml("status", Text("No task running"))
      }
    }

    <div>
      <div id="status">Waiting...</div>
      <div>{Script(OnLoad(PeriodicUpdate(update, 1000)))}</div>
    </div>
  }
}