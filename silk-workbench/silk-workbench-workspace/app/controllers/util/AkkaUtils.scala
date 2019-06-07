package controllers.util

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import org.silkframework.runtime.activity.Observable
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket

import scala.collection.mutable

object AkkaUtils {

  // Keeps track of all active subscribers
  private val activeSubscribers = mutable.WeakHashMap.empty[Source[_, _], _ => Unit]

  /**
    * Creates an Akka Source from a Silk Observable
    */
  def createSource[T](observable: Observable[T])
                     (implicit mat: Materializer): Source[T, _] = {
    // Create new source
    val source = Source.actorRef(1, OverflowStrategy.dropHead)
    val (outActor, publisher) = source.toMat(Sink.asPublisher(false))(Keep.both).run()

    // Push updates into source
    val subscriber = (value: T) => outActor ! value
    for(initialValue <- observable.get) {
      outActor ! initialValue
    }
    observable.subscribe(subscriber)

    // Make sure that the subscriber is not garbage collected
    activeSubscribers.synchronized {
      activeSubscribers.put(source, subscriber)
    }

    Source.fromPublisher(publisher)
  }

  def createWebSocket[T](flow: Flow[JsValue, JsValue, _]): WebSocket = {
    WebSocket.accept[JsValue, JsValue] { request => flow }
  }
}
