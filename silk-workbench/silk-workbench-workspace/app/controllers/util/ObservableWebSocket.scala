package controllers.util

import java.util.logging.Logger
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import org.silkframework.runtime.activity.Observable
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket

object ObservableWebSocket {

  def create[T](observable: Observable[T])
               (implicit format: JsonFormat[T], system: ActorSystem, mat: Materializer): WebSocket = {
    WebSocket.accept[JsValue, JsValue] { request =>
      ActorFlow.actorRef { out =>
        Props(new ObservableWebSocket(out, observable))
      }
    }
  }
}

private class ObservableWebSocket[T](out: ActorRef, observable: Observable[T])(implicit format: JsonFormat[T]) extends Actor {

  private val log: Logger = Logger.getLogger(getClass.getName)

  observable.subscribe(Subscriber)

  private implicit val writeContext = WriteContext[JsValue]()

  def receive = {
    case msg =>
      log.info("Received unexpected WebSocket message: " + msg)
  }

  object Subscriber extends (T => Unit) {
    override def apply(value: T): Unit = {
      out ! format.write(value)
    }
  }

}