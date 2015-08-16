package io.github.drewnoff.inapp.api

import spray.routing.Directives
import scala.concurrent.ExecutionContext
import akka.actor.ActorRef
import io.github.drewnoff.inapp.core.{InApp, InAppRegistryActor}
import io.github.drewnoff.inapp.core.InAppRegistryActor.{
  Register, Registered, NotRegistered, ListAll, RegistryList,
  Delete, Deleted, GetInApp
}
import akka.util.Timeout
import spray.http._

import spray.json.{DefaultJsonProtocol}
import spray.routing.HttpService
import spray.httpx.SprayJsonSupport._

import io.github.drewnoff.inapp.core.InApp
import scala.Some


trait InAppRegistryRouteFormats extends DomainJsonFormats {
  implicit val registerFormat = jsonFormat1(Register)
  implicit val registeredFormat = jsonFormat1(Registered)
  implicit val notRegisteredFormat = jsonObjectFormat[NotRegistered.type]
  implicit val listAllFormat = jsonObjectFormat[ListAll.type]
  implicit val registryListFormat = jsonFormat1(RegistryList)
  implicit val deleteFormat = jsonFormat1(Delete)
  implicit val deletedFormat = jsonFormat1(Deleted)
  implicit val getInAppFormat = jsonFormat1(GetInApp)
  implicit object EitherErrorSelector extends ErrorSelector[NotRegistered.type] {
    def apply(v: NotRegistered.type): StatusCode = StatusCodes.BadRequest
  }
}

trait InAppRegistryRoute extends Directives with InAppRegistryRouteFormats {

  def inappregistry: ActorRef
  implicit def executionContext: ExecutionContext

  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val timeout = Timeout(4.seconds)

  import scala.concurrent.future

  val route = pathPrefix("api" / "v1" / "inapps") {
    path("_id" / Rest) { id =>
      get {
         complete(
            for {
            result <- (inappregistry ? GetInApp(id)).mapTo[InApp]
          } yield result
        )
      } ~ delete {
        complete(
          for {
            result <- (inappregistry ? Delete(id)).mapTo[Deleted]
          } yield result
        )
      }
    } ~ pathEnd {
      get {
        complete (
          for {
            result <- (inappregistry ? ListAll).mapTo[RegistryList]
          } yield result
        )
      } ~
      post {
        handleWith { inapp: InApp =>
          for {
            result <- (
              inappregistry ? Register(inapp)
            ).mapTo[Registered]
          } yield result
        }
      }
    }
  }
}

class InAppRegistryService(inappregistryActorRef: ActorRef)(implicit callerExecutionContex: ExecutionContext) extends InAppRegistryRoute {
  val inappregistry = inappregistryActorRef
  implicit def executionContext = callerExecutionContex
  import ExecutionContext.Implicits.global
}
