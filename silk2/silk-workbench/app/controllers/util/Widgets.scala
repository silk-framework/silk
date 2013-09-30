package controllers.util

import play.api.libs.iteratee.Enumerator
import de.fuberlin.wiwiss.silk.util.task.TaskStatus
import play.api.libs.Comet
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

object Widgets {
  val log = java.util.logging.Logger.getLogger(getClass.getName)

  def taskStatus(stream: Enumerator[TaskStatus], id: String = "") = {
    def serializeStatus(status: TaskStatus): JsValue = {
      JsObject(
        ("id" -> JsString(id)) ::
        ("progress" -> JsNumber(status.progress * 100.0)) ::
        ("message" -> JsString(status.toString)) ::
        ("failed" -> JsBoolean(status.failed)) :: Nil
      )
    }
    stream.map(serializeStatus) &> Comet(callback = "parent.updateStatus")
  }

  def autoReload(reloadFunc: String, stream: Enumerator[_]) = {
    stream.map(_ => "") &> Comet(callback = "parent." + reloadFunc)
  }
}