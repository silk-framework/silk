package controllers.util

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props, Status, Terminated}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.CompactByteString
import org.silkframework.runtime.activity.Observable
import play.api.http.websocket.{Message, PingMessage}
import play.api.libs.json.JsValue
import play.api.mvc.{RequestHeader, Result, WebSocket}
import scala.concurrent.duration._
import scala.concurrent.Future

object AkkaUtils {

  /**
    * Creates an Akka Source from a Silk Observable
    */
  def createSource[T](observable: Observable[T])
                     (implicit system: ActorSystem, mat: Materializer): Source[T, _] = {
    // Create new source
    val actorSource = Source.actorRef(1, OverflowStrategy.dropHead)
    val (outActor, publisher) = actorSource.toMat(Sink.asPublisher(false))(Keep.both).run()

    // Subscribe to updates
    system.actorOf(Props(new SubscriberActor(observable, outActor)))

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

  /**
    * Actor that subscribes to an observable and forwards all updates to another actor.
    */
  private class SubscriberActor[T](observable: Observable[T], outActor: ActorRef) extends Actor {

    // We need to stop sending if the outActor terminates
    context.watch(outActor)

    // Subscribe to observable and push current value
    private val subscriber = (value: T) => { outActor ! value }
    observable.subscribe(subscriber)
    // Push current value
    for(initialValue <- observable.get) {
      outActor ! initialValue
    }

    override def receive: Receive = {
      case Status.Success(_) | Status.Failure(_) =>
        observable.removeSubscription(subscriber)
        outActor ! PoisonPill
      case Terminated(_) =>
        observable.removeSubscription(subscriber)
        context.stop(self)
      case other: Any =>
        outActor ! other
    }
  }
}
