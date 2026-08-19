package controllers.util

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props, Status, Terminated}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.CompactByteString
import org.silkframework.runtime.activity.{Observable, UserContext}
import org.silkframework.runtime.users.WebUserManager
import org.silkframework.runtime.validation.RequestException
import org.silkframework.workbench.utils.ErrorResult
import play.api.http.websocket.{Message, PingMessage}
import play.api.libs.json.JsValue
import play.api.mvc.{RequestHeader, Result, WebSocket}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object AkkaUtils {

  /**
    * Creates an Akka Source from a Silk Observable
    */
  def createSource[T](observable: Observable[T], maxFrequency: Option[FiniteDuration] = None)
                     (implicit system: ActorSystem, mat: Materializer): Source[T, _] = {
    // Create new source
    var actorSource = Source.actorRef(
      completionMatcher = PartialFunction.empty,
      failureMatcher = PartialFunction.empty,
      bufferSize = 1,
      overflowStrategy = OverflowStrategy.dropHead
    )
    for(duration <- maxFrequency) {
      actorSource = actorSource.throttle(1, duration)
    }

    val (outActor, publisher) = actorSource.toMat(Sink.asPublisher(false))(Keep.both).run()

    // Subscribe to updates
    system.actorOf(Props(new SubscriberActor(observable, outActor)))

    Source.fromPublisher(publisher)
  }

  /**
    * Creates a WebSocket from a JSON source that is built for the requesting user.
    * The source must be created inside the request, since only there a user context is available. Building it upfront
    * would also raise access control errors while the controller method is evaluated, i.e. as a 500 instead of a 403.
    */
  def createWebSocket(createSource: UserContext => Source[JsValue, _]): WebSocket = {
    new WebSocket {
      override def apply(request: RequestHeader): Future[Either[Result, Flow[Message, Message, _]]] = {
        WebUserManager().webSocketUserContext(request).map {
          case Right(userContext) =>
            try {
              Right(keepAliveFlow(createSource(userContext)))
            } catch {
              case ex: RequestException =>
                Left(ErrorResult(ex))
            }
          case Left(rejection) =>
            Left(rejection)
        }(ExecutionContext.parasitic)
      }
    }
  }

  private def keepAliveFlow(source: Source[JsValue, _]): Flow[Message, Message, _] = {
    val jsonFlow = Flow.fromSinkAndSource(Sink.ignore, source)
    val messageFlow = WebSocket.MessageFlowTransformer.jsonMessageFlowTransformer.transform(jsonFlow)

    /**
      * Keep the connection alive.
      * This could also be achieved automatically by setting the 'akka.http.server.websocket.periodic-keep-alive-max-idle' parameter
      * But setting this using Play is cumbersome...
      */
    messageFlow.keepAlive(10.seconds, () => PingMessage(CompactByteString()))
  }

  /**
    * Actor that subscribes to an observable and forwards all updates to another actor.
    */
  private class SubscriberActor[T](observable: Observable[T], outActor: ActorRef) extends Actor {

    // We need to stop sending if the outActor terminates
    context.watch(outActor)

    // Subscribe to observable
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
