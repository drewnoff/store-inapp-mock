package io.github.drewnoff.inapp.api

import spray.routing.Directives
import scala.concurrent.ExecutionContext
import akka.actor.ActorRef
import io.github.drewnoff.inapp.core.{
  Subscription, SchedulerActor, SubscriptionPeriod, Paid, Trial, Lapse}
import io.github.drewnoff.inapp.core.AccountantActor._
import akka.util.Timeout
import spray.http._

import spray.json._
import spray.routing.HttpService
import spray.httpx.SprayJsonSupport._


trait AccountantRouteFormats extends DomainJsonFormats {

  implicit val validateFormat = jsonFormat1(Validate)
  implicit val validFormat = jsonFormat4(Valid)
  implicit val invalidFormat = jsonFormat2(Invalid)
  implicit val restoreFormat = jsonFormat2(Restore)
  implicit val restoredFormat = jsonFormat1(Restored)
  implicit val notRestoredFormat = jsonObjectFormat[NotRestored.type]
}

trait AccountantRoute extends Directives with AccountantRouteFormats {

  def accountant: ActorRef
  implicit def executionContext: ExecutionContext

  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val timeout = Timeout(4.seconds)

  val route = pathPrefix("api" / "v1" / "inapps" / "receipts") {
    path("restore") {
      post {
        handleWith { rstore: Restore =>
          for {
            result <- (
              accountant ? rstore
            ).mapTo[Restored] // TODO  look at SchedulerService TODOs
          } yield result
        }
      }// TODO add Validate call handler
    } ~path("verifyReceipt") {
      post {
        handleWith { vl: Validate =>
          for {
            result <- (
              accountant ? vl
            ).mapTo[Valid] // TODO  look at SchedulerService TODOs
          } yield result
        } // TODO add Validate call handler
      }
    }
  }
}

class AccountantService(accountantActorRef: ActorRef)(implicit callerExecutionContex: ExecutionContext) extends AccountantRoute {
  val accountant = accountantActorRef
  implicit def executionContext = callerExecutionContex
  import ExecutionContext.Implicits.global
}
