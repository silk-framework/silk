package controllers.util

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.CompactByteString
import org.silkframework.runtime.activity.Observable
import play.api.http.websocket.{Message, PingMessage}
import play.api.libs.json.JsValue
import play.api.mvc.{RequestHeader, Result, WebSocket}

import scala.concurrent.duration._
import scala.collection.mutable
import scala.concurrent.Future

object AkkaUtils {

  // Keeps track of all active subscribers
  private val activeSubscribers = mutable.WeakHashMap.empty[Source[_, _], _ => Unit]

  /**
    * Creates an Akka Source from a Silk Observable
    */
  def createSource[T](observable: Observable[T])
                     (implicit mat: Materializer): Source[T, _] = {
    // Create new source
    val actorSource = Source.actorRef(1, OverflowStrategy.dropHead)
    val (outActor, publisher) = actorSource.toMat(Sink.asPublisher(false))(Keep.both).run()

    // Push updates into source
    val subscriber = (value: T) => outActor ! value
    for(initialValue <- observable.get) {
      outActor ! initialValue
    }
    observable.subscribe(subscriber)

    // Make sure that the subscriber is not garbage collected
    activeSubscribers.synchronized {
      activeSubscribers.put(actorSource, subscriber)
    }

    Source.fromPublisher(publisher)
  }

  /**
    * Creates a WebSocket from a JSON source.
    */
  def createWebSocket(source: Source[JsValue, _]): WebSocket = {
    val jsonFlow = Flow.fromSinkAndSource(Sink.ignore, source)
    val messageFlow = WebSocket.MessageFlowTransformer.jsonMessageFlowTransformer.transform(jsonFlow)

    /**
      * Keep the connection alive.
      * This could also be achieved automatically by setting the 'akka.http.server.websocket.periodic-keep-alive-max-idle' parameter
      * But setting this using Play is cumbersome...
      */
    val keepAliveFlow = messageFlow.keepAlive(10.seconds, () => PingMessage(CompactByteString()))

    new WebSocket {
      override def apply(request: RequestHeader): Future[Either[Result, Flow[Message, Message, _]]] = {
        Future.successful(Right(keepAliveFlow))
      }
    }
  }
}
