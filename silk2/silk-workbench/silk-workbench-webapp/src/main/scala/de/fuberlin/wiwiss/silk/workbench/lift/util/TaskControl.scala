package de.fuberlin.wiwiss.silk.workbench.lift.util

import de.fuberlin.wiwiss.silk.util.Task
import collection.mutable.{Publisher, Subscriber}
import java.util.UUID
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.{SHtml, CometActor}
import de.fuberlin.wiwiss.silk.util.Task.{Started, Finished}
import xml.Text

class TaskControl(task : Task[_], cancelable : Boolean = false) extends CometActor with Subscriber[Task.StatusMessage, Publisher[Task.StatusMessage]]
{
  task.subscribe(this)

  private val id = UUID.randomUUID.toString

  override def notify(pub : Publisher[Task.StatusMessage], status : Task.StatusMessage)
  {
    status match
    {
      case Started() if cancelable => updateButton("Cancel")
      case Started()               => updateButton("Start")
      case Finished(_, _)          => updateButton("Start")
      case _                       =>
    }
  }

  override def render =
  {
    def buttonPressed() : JsCmd =
    {
      if(!task.isRunning)
      {
        task.runInBackground()
      }
      else if(cancelable)
      {
        task.cancel()
      }

      JS.Empty
    }

    SHtml.ajaxButton(Text("Start"), buttonPressed _, ("id" -> id))
  }

  private def updateButton(newValue : String, enabled : Boolean = true)
  {
    partialUpdate(JsRaw("$('#" + id + " span').html('" + newValue + "')").cmd)
  }
}