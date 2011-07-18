package de.fuberlin.wiwiss.silk.workbench.lift.util

import collection.mutable.{Publisher, Subscriber}
import java.util.UUID
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.{SHtml, CometActor}
import xml.Text
import de.fuberlin.wiwiss.silk.util.task.{Status, Finished, Started, Task}

class TaskControl(task : Task[_], cancelable : Boolean = false) extends CometActor with Subscriber[Status, Publisher[Status]]
{
  task.subscribe(this)

  private val id = UUID.randomUUID.toString

  override def notify(pub : Publisher[Status], status : Status)
  {
    status match
    {
      case _ : Started if cancelable => updateButton("Cancel")
      case _ : Started               => updateButton("Start")
      case _ : Finished              => updateButton("Start")
      case _                         =>
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