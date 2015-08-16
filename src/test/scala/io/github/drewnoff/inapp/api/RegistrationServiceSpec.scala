package io.github.drewnoff.inapp.api

import org.scalatest.WordSpec
import org.scalatest.BeforeAndAfter
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import akka.actor.ActorRef
import spray.routing.Directives
import spray.http.HttpResponse
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest
import spray.routing.HttpService

import java.util.UUID
import io.github.drewnoff.inapp.core.User
import io.github.drewnoff.inapp.core.{CoreActors, Core}
import io.github.drewnoff.inapp.core.RegistrationActor.{
  Register, Registered, NotRegistered}

import com.mongodb.casbah.MongoClient


class RegistrationServiceSpec extends WordSpec with HttpService
    with ScalatestRouteTest with CoreActors
    with Core with RegistrationRoute with BeforeAndAfter {

  def actorRefFactory = system
  implicit def executionContext = global

  val client = MongoClient("localhost", 27017)
  val db = client("inapps")
  val collection = db("users")

  before {
    collection.drop()
  }

  val uri = "/api/v1/users"

  "Registration API" when {
    "using routing infrastructure" should {
      "support the most simple and direct route" in {
        Get(uri) ~> complete(HttpResponse()) ~> check {
          assert(response === HttpResponse())
        }
      }
    }

    s"POST $uri/register" should {
      val expected = User(UUID.randomUUID())

      "return OK" in {
        Post(s"$uri/register", Register(expected)) ~> route ~> check {
          assert(response.status === OK)
        }
      }
    }
  }
}
