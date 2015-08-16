package io.github.drewnoff.inapp.api

import spray.routing.Directives
import scala.concurrent.ExecutionContext
import akka.actor.ActorRef
import io.github.drewnoff.inapp.core.{
  Subscription, SchedulerActor, SubscriptionPeriod, Paid, Trial, Lapse}
import io.github.drewnoff.inapp.core.SchedulerActor._
import akka.util.Timeout
import spray.http._

import spray.json._
import spray.routing.HttpService
import spray.httpx.SprayJsonSupport._


trait SchedulerRouteFormats extends DomainJsonFormats {
  /**
    *  Instance of the ``RootJsonFormat`` for the ``io.github.drewnoff.inapp.core.SubscriptionPeriod``
    */
  implicit val subscribeFormat = jsonFormat3(Subscribe)
  implicit val subscribedFormat = jsonObjectFormat[Subscribed.type]
  implicit val alreadySubscribedFormat = jsonObjectFormat[AlreadySubscribed.type]
  implicit val unsubscribeFormat = jsonFormat2(Unsubscribe)
  implicit val unsubscribedFormat = jsonObjectFormat[Unsubscribed.type]
  implicit val notSubscribedFormat = jsonObjectFormat[NotSubscribed.type]

  implicit object SubscribeResultJsonFormat extends JsonFormat[SubscribeResult] {
    def write(x: SubscribeResult) = x match {
      case Subscribed => Subscribed.toJson
      case AlreadySubscribed => AlreadySubscribed.toJson
      case NotSubscribed => NotSubscribed.toJson
    }
    def read(value: JsValue) = value match {
      case JsObject(x) => x.toList match {
        case List(("value", JsString("Subscribed$"))) => Subscribed
        case List(("value", JsString("AlreadySubscribed"))) => AlreadySubscribed
        case List(("value", JsString("NotSubscribed"))) => NotSubscribed
      }
      case x           => deserializationError("Expected SubscribeResult as JsValue, but got " + x)
    }
  }


}

trait SchedulerRoute extends Directives with SchedulerRouteFormats {

  def scheduler: ActorRef
  implicit def executionContext: ExecutionContext

  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val timeout = Timeout(4.seconds)

  import scala.concurrent.future

  val route = pathPrefix("api" / "v1" / "inapps" / "subs") {
    path("subscribe") {
      post {
        handleWith { subr: Subscribe =>
          for {
            result <- (
              scheduler ? subr
            ).mapTo[Subscribed.type] // TODO should be SubscribedResult but could not find implicit value for parameter marshaller: spray.httpx.marshalling.ToResponseMarshaller[io.github.drewnoff.inapp.core.SchedulerActor.SubscribeResult]
          } yield result
        } // TODO add Unsubscribe call handler
      }
    }
  }
}

class SchedulerService(schedulerActorRef: ActorRef)(implicit callerExecutionContex: ExecutionContext) extends SchedulerRoute {
  val scheduler = schedulerActorRef
  implicit def executionContext = callerExecutionContex
  import ExecutionContext.Implicits.global
}
