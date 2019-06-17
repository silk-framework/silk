package controllers.core

import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.silkframework.runtime.activity.Status
import play.api.libs.Comet
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.streams.IterateeStreams
import play.api.libs.json._

object Widgets {

  def statusStream(stream: Enumerator[Status],
                   id: String = "progress",
                   project: String = "",
                   task: String = "",
                   activity: String = ""): Source[ByteString, _] = {

    def serializeStatus(status: Status): JsValue = {
      JsObject(
        ("id" -> JsString(id)) :: // TODO id can be deleted
        ("project" -> JsString(project)) ::
        ("task" -> JsString(task)) ::
        ("activity" -> JsString(activity)) ::
        ("statusName" -> JsString(status.name)) ::
        ("isRunning" -> JsBoolean(status.isRunning)) ::
        ("progress" -> status.progress.map(p => JsNumber(p * 100.0)).getOrElse(JsNull)) ::
        ("message" -> JsString(status.toString)) ::
        ("failed" -> JsBoolean(status.failed)) :: Nil
      )
    }
    convert(stream).map(serializeStatus) via Comet.json("parent.updateStatus")
  }

  def autoReload(reloadFunc: String, stream: Enumerator[_]): Source[ByteString, _]  = {
    convert(stream).map(_ => "") via Comet.string("parent." + reloadFunc)
  }

  /**
    * Converts a Play Enumerator to a Akka Source.
    * In the future, we should replace all uses of Enumerators with Akka Sources.
    */
  private def convert[T](enumerator: Enumerator[T]): Source[T, _] = {
    Source.fromPublisher(IterateeStreams.enumeratorToPublisher(enumerator))
  }
}