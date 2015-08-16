package io.github.drewnoff.inapp.api

import spray.routing.Directives
import scala.concurrent.ExecutionContext
import akka.actor.ActorRef
import io.github.drewnoff.inapp.core.{User, RegistrationActor}
import io.github.drewnoff.inapp.core.RegistrationActor.{Register,
  Registered, NotRegistered}
import akka.util.Timeout
import spray.http._

import spray.json.DefaultJsonProtocol
import spray.routing.HttpService
import spray.httpx.SprayJsonSupport._

import scala.Some


trait RegistrationRouteFormats extends DefaultJsonFormats {
  implicit val userFormat = jsonFormat1(User)
  implicit val registerFormat = jsonFormat1(Register)
  implicit val registeredFormat = jsonObjectFormat[Registered.type]
  implicit val notRegisteredFormat = jsonObjectFormat[NotRegistered.type]
  implicit object EitherErrorSelector extends ErrorSelector[NotRegistered.type] {
    def apply(v: NotRegistered.type): StatusCode = StatusCodes.BadRequest
  }
}

trait RegistrationRoute extends Directives with RegistrationRouteFormats {

  def registration: ActorRef
  implicit def executionContext: ExecutionContext

  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val timeout = Timeout(4.seconds)

  val route = pathPrefix("api" / "v1" / "users") {
    path("register") {
      post {
        handleWith { ru: Register =>
          for {
            result <- (registration ? ru).mapTo[Either[NotRegistered.type, Registered.type]]
          } yield result
        }
      }
    }
  }
}

class RegistrationService(registrationActorRef: ActorRef)(implicit callerExecutionContex: ExecutionContext) extends RegistrationRoute {
  val registration = registrationActorRef
  implicit def executionContext = callerExecutionContex
  import ExecutionContext.Implicits.global
}
