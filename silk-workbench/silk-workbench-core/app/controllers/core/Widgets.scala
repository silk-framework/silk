package controllers.core

import de.fuberlin.wiwiss.silk.runtime.activity.Status
import play.api.libs.iteratee.Enumerator
import play.api.libs.Comet
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

object Widgets {
  val log = java.util.logging.Logger.getLogger(getClass.getName)

  def status(stream: Enumerator[Status], id: String = "progress") = {
    def serializeStatus(status: Status): JsValue = {
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